package cz.tul.comm.messaging.job;

import cz.tul.comm.IService;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.communicator.Status;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.server.IClientManager;
import cz.tul.comm.server.IDataStorage;
import cz.tul.comm.socket.IListenerRegistrator;
import cz.tul.comm.socket.queue.IIdentifiable;
import cz.tul.comm.socket.queue.IListener;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class JobManager extends Thread implements IService, IListener {

    private static final Logger log = Logger.getLogger(JobManager.class.getName());
    private static final int WAIT_TIME = 1_000;
    private static final int MAX_CLIENT_NA_TIME = 5_000;
    private static final int MAX_JOB_ASSIGN_TIME = 5_000;
    private final IClientManager clientManager;
    private IDataStorage dataStorage;
    private final IListenerRegistrator listenerRegistrator;
    private final Deque<ServerSideJob> jobQueue;
    private final Map<Communicator, ServerSideJob> jobsWaitingAssignment;
    private final Map<Communicator, ServerSideJob> jobComputing;
    private final Map<Communicator, Calendar> lastTimeOnline;
    private final Map<ServerSideJob, Calendar> assignTime;
    private final Collection<Job> allJobs;
    private final Collection<Job> allJobsOuter;
    private boolean run;

    /**
     * Prepare new instance of JobManager.
     *
     * @param clientManager client manager
     * @param listenerRegistrator listener registrator
     */
    public JobManager(final IClientManager clientManager, final IListenerRegistrator listenerRegistrator) {
        this.clientManager = clientManager;
        this.listenerRegistrator = listenerRegistrator;
        jobsWaitingAssignment = new ConcurrentHashMap<>();
        jobComputing = new ConcurrentHashMap<>();
        lastTimeOnline = new ConcurrentHashMap<>();
        assignTime = new ConcurrentHashMap<>();
        jobQueue = new ConcurrentLinkedDeque<>();
        allJobs = new LinkedList<>();
        allJobsOuter = Collections.unmodifiableCollection(allJobs);

        run = true;
    }

    /**
     * Assign data storage
     *
     * @param dataStorage class handling data requests
     */
    public void setDataStorage(IDataStorage dataStorage) {
        this.dataStorage = dataStorage;
    }

    /**
     * Submit job for computation
     *
     * @param task jobs task
     * @return interface for job control
     */
    public Job submitJob(final Object task) {
        final ServerSideJob result = new ServerSideJob(task, listenerRegistrator, dataStorage);
        jobQueue.add(result);
        allJobs.add(result);

        log.log(Level.CONFIG, "Job with ID {0} submitted.", result.getId());
        wakeUp();

        return result;
    }

    public void waitForAllJobs() {
        while (!jobQueue.isEmpty() && !jobsWaitingAssignment.isEmpty()) {
            synchronized (this) {
                try {
                    this.wait();
                } catch (InterruptedException ex) {
                    log.log(Level.WARNING, "Waiting for all jobs to complete failed.", ex);
                }
            }
        }
    }

    public void stopAllJobs() {
        jobQueue.clear();

        for (ServerSideJob ssj : jobsWaitingAssignment.values()) {
            ssj.cancelJob();
        }
        jobsWaitingAssignment.clear();

        for (ServerSideJob ssj : jobsWaitingAssignment.values()) {
            ssj.cancelJob();
        }
        jobComputing.clear();

        lastTimeOnline.clear();
        assignTime.clear();
    }

    public Collection<Job> getAllJobs() {
        return allJobsOuter;
    }

    @Override
    public void run() {
        log.fine("JobManager has been started.");
        while (run) {
            if (!jobQueue.isEmpty()) {
                assignJobs();
                checkAssignedJobs();
                checkClientStatuses();
            }
            synchronized (this) {
                try {
                    this.wait(WAIT_TIME);
                } catch (InterruptedException ex) {
                    log.log(Level.WARNING, "Waiting of JobManager has been interrupted.", ex);
                }
            }
        }
    }

    private void assignJobs() {
        ServerSideJob job;
        final Collection<Communicator> clients = clientManager.getClients();
        for (Communicator comm : clients) {
            if (jobQueue.isEmpty()) {
                break;
            }
            if (!isClientOnline(comm)
                    || jobsWaitingAssignment.get(comm) != null
                    || jobComputing.get(comm) != null) {
                // no connection or client has assigned unconfirmed job pending 
                // or client is computing
                continue;
            }

            // new assignment
            job = jobQueue.poll();
            log.log(Level.CONFIG, "Job with ID {0} assigned to client with ID {1}.", new Object[]{job.getId(), comm.getId()});
            assignJob(job, comm);            
        }
    }

    private boolean isClientOnline(final Communicator comm) {
        final Status s = comm.getStatus();
        final boolean result = s.equals(Status.ONLINE) || s.equals(Status.REACHABLE) || s.equals(Status.BUSY);
        if (result) {
            storeClientOnlineStatus(comm);
        }
        return result;
    }

    private void storeClientOnlineStatus(final Communicator comm) {
        lastTimeOnline.put(comm, Calendar.getInstance());
    }

    private void assignJob(final ServerSideJob job, final Communicator comm) {
        listenerRegistrator.addIdListener(job.getId(), this, true);
        jobsWaitingAssignment.put(comm, job);       
        storeClientOnlineStatus(comm);        
        job.setStatus(JobStatus.SENT);
        assignTime.put(job, Calendar.getInstance());
        job.submitJob(comm);
    }

    private void checkAssignedJobs() {
        final long time = Calendar.getInstance().getTimeInMillis();
        final Set<ServerSideJob> reassign = new HashSet<>();

        long dif;
        for (Entry<ServerSideJob, Calendar> e : assignTime.entrySet()) {
            dif = time - e.getValue().getTimeInMillis();
            if (dif > MAX_JOB_ASSIGN_TIME) {
                reassign.add(e.getKey());
            }
        }

        Communicator comm;
        for (ServerSideJob ssj : reassign) {
            ssj.cancelJob();
            // check client, then resend or give to another
            comm = ssj.getComm();
            if (isClientOnline(comm)) {
                ssj.submitJob(comm);
            } else {
                jobsWaitingAssignment.remove(comm);
                if (!jobQueue.contains(ssj)) {
                    jobQueue.addFirst(ssj);
                }
            }
        }
    }

    private void checkClientStatuses() {
        for (Communicator comm : jobsWaitingAssignment.keySet()) {
            if (!isClientOnline(comm)) {
                final Calendar actualTime = Calendar.getInstance();
                final Calendar lastOnline = lastTimeOnline.get(comm);
                if (lastOnline != null) {
                    final long dif = actualTime.getTimeInMillis() - lastOnline.getTimeInMillis();
                    if (dif > MAX_CLIENT_NA_TIME) {
                        final ServerSideJob reassigned = jobsWaitingAssignment.get(comm);
                        if (!jobQueue.contains(reassigned)) {
                            jobQueue.add(reassigned);
                        }
                    }
                } else {
                    log.severe("No online time recorder for job communicator.");
                }
            }
        }
    }

    @Override
    public void stopService() {
        run = false;
        stopAllJobs();

        synchronized (this) {
            this.notify();
        }

        log.fine("JobManager has been stopped.");
    }

    @Override
    public void receiveData(final IIdentifiable data) {
        if (data instanceof Message) {
            final Message m = (Message) data;
            final UUID id = m.getId();
            ServerSideJob ssj = null;

            switch (m.getHeader()) {
                case JobMessageHeaders.JOB_ACCEPT:
                    for (Entry<Communicator, ServerSideJob> e : jobsWaitingAssignment.entrySet()) {
                        if (e.getValue().getId().equals(id)) {
                            ssj = e.getValue();
                            jobsWaitingAssignment.remove(e.getKey());
                            jobComputing.put(e.getKey(), ssj);
                            log.log(Level.FINE, "Job with ID {0} has been accepted.", id);
                            break;
                        }
                    }
                    if (ssj == null) {
                        log.log(Level.WARNING, "JobAccept received for illegal UUID - {0}", id);
                    }
                    break;
                case JobMessageHeaders.JOB_CANCEL:
                    for (Entry<Communicator, ServerSideJob> e : jobComputing.entrySet()) {
                        if (e.getValue().getId().equals(id)) {
                            ssj = e.getValue();
                            jobComputing.remove(e.getKey());
                            jobQueue.addFirst(ssj);
                            log.log(Level.CONFIG, "Job with ID {0} has been cancelled.", id);
                            wakeUp();
                            break;
                        }
                    }
                    if (ssj == null) {
                        log.log(Level.WARNING, "JobCancel received for illegal UUID - {0}", id);
                    }
                    break;
                case JobMessageHeaders.JOB_RESULT:
                    for (Entry<Communicator, ServerSideJob> e : jobComputing.entrySet()) {
                        if (e.getValue().getId().equals(id)) {
                            ssj = e.getValue();
                            jobComputing.remove(e.getKey());
                            log.log(Level.CONFIG, "Job with ID {0} has been computed succefully.", id);
                            wakeUp();
                            break;
                        }
                    }
                    if (ssj == null) {
                        log.log(Level.WARNING, "JobResult received for illegal UUID - {0}", id);
                    }
                    break;
            }
        }
    }

    private void wakeUp() {
        synchronized (this) {
            this.notify();
        }
    }
}
