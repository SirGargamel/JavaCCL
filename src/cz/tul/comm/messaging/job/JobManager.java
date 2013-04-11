package cz.tul.comm.messaging.job;

import cz.tul.comm.IService;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.communicator.Status;
import cz.tul.comm.server.IClientManager;
import cz.tul.comm.server.IDataStorage;
import cz.tul.comm.socket.IListenerRegistrator;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class JobManager extends Thread implements IService {

    private static final Logger log = Logger.getLogger(JobManager.class.getName());
    private static final int WAIT_TIME = 1_000;
    private static final int MAX_CLIENT_NA_TIME = 5_000;
    private static final int MAX_JOB_ASSIGN_TIME = 5_000;
    private final IClientManager clientManager;
    private IDataStorage dataStorage;
    private final IListenerRegistrator listenerRegistrator;
    private final Queue<ServerSideJob> jobQueue;
    private final Map<Communicator, ServerSideJob> jobAssignment;
    private final Map<Communicator, Calendar> lastTimeOnline;
    private final Map<ServerSideJob, Calendar> assignTime;   
    private final Set<Job> jobs;
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
        jobAssignment = new ConcurrentHashMap<>();
        lastTimeOnline = new HashMap<>();
        assignTime = new HashMap<>();
        jobQueue = new ConcurrentLinkedQueue<>();       
        jobs = new HashSet<>();

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
        jobs.add(result);

        log.log(Level.CONFIG, "Job with ID {0} submitted.", result.getId());
        synchronized (this) {
            this.notify();
        }

        return result;
    }
    
    public void waitForJobs() {
        while (!jobQueue.isEmpty() && !jobAssignment.isEmpty()) {
            synchronized (this) {
                try {
                    this.wait(WAIT_TIME);
                } catch (InterruptedException ex) {
                    log.log(Level.WARNING, "Waiting for all jobs to complete failed.", ex);
                }
            }
        }
    }
    
    public void clearAllJobs() {
        jobQueue.clear();
        jobAssignment.clear();
        lastTimeOnline.clear();
        assignTime.clear();        
    }
    
    public Collection<Job> getAllJobs() {
        return Collections.unmodifiableCollection(jobs);
    }

    @Override
    public void run() {
        log.fine("JobManager has been started.");
        while (run) {
            if (!jobQueue.isEmpty()) {
                assignJobs();
                checkAssignedJobs();
                checkClients();
            }
            synchronized (this) {
                try {
                    this.wait(WAIT_TIME);
                } catch (InterruptedException ex) {
                    log.log(Level.WARNING, "Waiting of JobManager has been interrupted", ex);
                }
            }
        }
    }

    @Override
    public void stopService() {
        run = false;
        synchronized (this) {
            this.notify();
        }

        for (ServerSideJob ssj : jobAssignment.values()) {
            ssj.cancelJob();
        }

        log.fine("JobManager has been stopped.");
    }

    private void assignJobs() {
        Set<Communicator> clients;
        ServerSideJob job;
        clients = clientManager.getClients();
        for (Communicator comm : clients) {
            if (jobQueue.isEmpty() || !isClientOnline(comm)) {
                break;
            }
            job = jobAssignment.get(comm);
            if (job != null && job.isDone()) {
                // job cleanup
                log.log(Level.CONFIG, "Job with ID {0} found as done.", new Object[]{job.getId(), comm.getId()});
                jobAssignment.remove(comm);
                jobQueue.remove(job);                
                job = null;
            }

            if (job == null) {
                // new assignment
                job = jobQueue.poll();
                assignJob(job, comm);
                lastTimeOnline.put(comm, Calendar.getInstance());
                log.log(Level.CONFIG, "Job with ID {0} assigned to client with ID {1}.", new Object[]{job.getId(), comm.getId()});
            }
        }
    }

    private void checkClients() {
        for (Communicator comm : jobAssignment.keySet()) {
            if (!isClientOnline(comm)) {
                final Calendar actualTime = Calendar.getInstance();
                final Calendar lastOnline = lastTimeOnline.get(comm);
                if (lastOnline != null) {
                    final long dif = actualTime.getTimeInMillis() - lastOnline.getTimeInMillis();
                    if (dif > MAX_CLIENT_NA_TIME) {
                        final ServerSideJob reassigned = jobAssignment.get(comm);
                        if (!jobQueue.contains(reassigned)) {
                            jobQueue.add(reassigned);
                        }
                    }
                } else {
                    log.warning("No online time recorder for job communicator.");
                }
            } else {
                lastTimeOnline.put(comm, Calendar.getInstance());
            }
        }
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
            // check client, then resend or give to another
            comm = ssj.getComm();
            if (isClientOnline(comm)) {
                ssj.submitJob(comm);
            } else {
                ssj.cancelJob();
                jobAssignment.remove(comm);
                if (!jobQueue.contains(ssj)) {
                    jobQueue.add(ssj);
                }
            }
        }
    }

    private boolean isClientOnline(final Communicator comm) {
        final Status s = comm.getStatus();
        return (s.equals(Status.ONLINE) || s.equals(Status.REACHABLE) || s.equals(Status.BUSY));
    }

    private void assignJob(final ServerSideJob job, final Communicator comm) {
        job.submitJob(comm);
        jobAssignment.put(comm, job);
        assignTime.put(job, Calendar.getInstance());
    }
}
