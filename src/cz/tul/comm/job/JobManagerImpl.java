package cz.tul.comm.job;

import cz.tul.comm.IService;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.communicator.Status;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.messaging.MessageHeaders;
import cz.tul.comm.server.ClientManager;
import cz.tul.comm.server.DataStorage;
import cz.tul.comm.socket.ListenerRegistrator;
import cz.tul.comm.socket.queue.Identifiable;
import cz.tul.comm.socket.queue.Listener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class managing job assignment and management.
 *
 * @author Petr Ječmen
 */
public class JobManagerImpl extends Thread implements IService, Listener, JobRequestManager, JobManager {

    private static final Logger log = Logger.getLogger(JobManagerImpl.class.getName());
    private static final int WAIT_TIME = 1_000;
    private static final int MAX_CLIENT_NA_TIME = 5_000;
    private static final int MAX_JOB_ASSIGN_TIME = 5_000;
    private static final int JOB_CANCEL_WAIT_TIME = 2_000;

    private static void addJob(final Communicator comm, final ServerSideJob job, final Map<Communicator, List<ServerSideJob>> dataMap) {
        List<ServerSideJob> l = dataMap.get(comm);
        if (l == null) {
            l = new ArrayList<>(1);
            dataMap.put(comm, l);
        }
        l.add(job);
    }
    private final ClientManager clientManager;
    private DataStorage dataStorage;
    private final ListenerRegistrator listenerRegistrator;
    private final Deque<ServerSideJob> jobQueue;
    private final Map<Communicator, List<ServerSideJob>> jobsWaitingAssignment;
    private final Map<Communicator, List<ServerSideJob>> jobComputing;
    private final Map<Communicator, Calendar> lastTimeOnline;
    private final Map<ServerSideJob, Calendar> assignTime;
    private final Collection<Job> allJobs;
    private final Collection<Job> allJobsOuter;
    private final Map<UUID, List<JobAction>> jobHistory;
    private boolean run;

    /**
     * Prepare new instance.
     *
     * @param clientManager client manager
     * @param listenerRegistrator listener registrator
     */
    public JobManagerImpl(final ClientManager clientManager, final ListenerRegistrator listenerRegistrator) {
        this.clientManager = clientManager;
        this.listenerRegistrator = listenerRegistrator;
        jobsWaitingAssignment = new ConcurrentHashMap<>();
        jobComputing = new ConcurrentHashMap<>();
        lastTimeOnline = new ConcurrentHashMap<>();
        assignTime = new ConcurrentHashMap<>();
        jobQueue = new ConcurrentLinkedDeque<>();
        allJobs = new LinkedList<>();
        allJobsOuter = Collections.unmodifiableCollection(allJobs);
        jobHistory = new HashMap<>();

        run = true;
    }

    /**
     * Assign data storage
     *
     * @param dataStorage class handling data requests
     */
    public void setDataStorage(DataStorage dataStorage) {
        this.dataStorage = dataStorage;
    }

    @Override
    public Job submitJob(final Object task) {
        final ServerSideJob result = new ServerSideJob(task, listenerRegistrator, dataStorage);
        jobQueue.add(result);
        allJobs.add(result);

        log.log(Level.CONFIG, "Job with ID {0} submitted.", result.getId());
        wakeUp();

        return result;
    }

    @Override
    public void requestJob(final Communicator comm) {
        if (isClientOnline(comm)) {
            final ServerSideJob ssj = jobQueue.poll();
            log.log(Level.CONFIG, "Job with ID {0} assigned to client with ID {1} after request.", new Object[]{ssj.getId(), comm.getId()});
            assignJob(ssj, comm);
        }
    }

    @Override
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

    @Override
    public void stopAllJobs() {
        jobQueue.clear();

        for (List<ServerSideJob> l : jobsWaitingAssignment.values()) {
            for (ServerSideJob ssj : l) {
                ssj.cancelJob();
                storeJobAction(ssj.getId(), null, JobMessageHeaders.JOB_CANCEL);
            }
        }
        jobsWaitingAssignment.clear();

        for (List<ServerSideJob> l : jobComputing.values()) {
            for (ServerSideJob ssj : l) {
                ssj.cancelJob();
                storeJobAction(ssj.getId(), null, JobMessageHeaders.JOB_CANCEL);
            }
        }
        jobComputing.clear();

        lastTimeOnline.clear();
        assignTime.clear();
    }

    @Override
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
        final Collection<ServerSideJob> putBack = new LinkedList<>();
        Communicator comm;
        while (!jobQueue.isEmpty() && isAnyClientFree(clients)) {
            job = jobQueue.poll();
            comm = pickClient(job.getId(), clients);
            if (comm != null) {
                log.log(Level.CONFIG, "Job with ID {0} assigned to client with ID {1}.", new Object[]{job.getId(), comm.getId()});
                assignJob(job, comm);
            } else {
                putBack.add(job);
            }
        }

        for (ServerSideJob ssj : putBack) {
            jobQueue.addFirst(ssj);
        }
    }

    private boolean isAnyClientFree(final Collection<Communicator> clients) {
        for (Communicator comm : clients) {
            if (isClientFree(comm)) {
                return true;
            }
        }
        return false;
    }

    private boolean isClientFree(final Communicator comm) {
        return isClientOnline(comm)
                && jobsWaitingAssignment.get(comm) == null
                && jobComputing.get(comm) == null;
    }

    private Communicator pickClient(final UUID jobId, final Collection<Communicator> clients) {
        final Collection<Communicator> candidates = new LinkedList<>();
        // pick online only
        for (Communicator comm : clients) {
            if (isClientFree(comm)) {
                candidates.add(comm);
            }
        }
        // 1st round - no actions with job before
        List<JobAction> actionList = jobHistory.get(jobId);
        for (Communicator comm : candidates) {
            if (isCommPresent(actionList, comm)) {
                continue;
            }
            return comm;
        }
        // 2nd round - longest tim from last cancel
        Calendar cal;
        final SortedMap<Calendar, Communicator> cancelTimes = new TreeMap<>();
        for (Communicator comm : candidates) {
            cal = lastCancelTime(actionList, comm);
            if (cal != null) {
                cancelTimes.put(cal, comm);
            } else {
                log.warning("NULL calendar for last cancel time.");
            }
        }
        if (!cancelTimes.isEmpty()) {
            cal = cancelTimes.firstKey();
            if ((Calendar.getInstance(Locale.getDefault()).getTimeInMillis() - cal.getTimeInMillis()) > JOB_CANCEL_WAIT_TIME) {
                return cancelTimes.get(cal);
            } else {
                log.log(Level.FINE, "No communicator assigned for job {0}, too soon after last job cancel", jobId);
                return null;
            }
        } else {
            log.log(Level.FINE, "No communicator assigned for job {0}, no aviable found.", jobId);
            return null;
        }
    }

    private boolean isCommPresent(final List<JobAction> actionList, final Communicator comm) {
        if (actionList == null) {
            return false;
        }

        for (JobAction ja : actionList) {
            if (ja.getOwner() == comm) {
                return true;
            }
        }
        return false;
    }

    private Calendar lastCancelTime(final List<JobAction> actionList, final Communicator comm) {
        List<Calendar> cancelTimes = new ArrayList<>();
        for (JobAction ja : actionList) {
            if (ja.getOwner() == comm && ja.getActionDescription().equals(JobMessageHeaders.JOB_CANCEL)) {
                cancelTimes.add(ja.getActionTime());
            }
        }
        if (cancelTimes.size() > 0) {
            Collections.sort(cancelTimes);
            return cancelTimes.get(cancelTimes.size() - 1);
        } else {
            return null;
        }
    }

    private boolean isClientOnline(final Communicator comm) {
        final Status s = comm.getStatus();
        final boolean result = s.equals(Status.ONLINE) || s.equals(Status.REACHABLE);
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
        addJob(comm, job, jobsWaitingAssignment);
        storeClientOnlineStatus(comm);
        job.setStatus(JobStatus.SENT);
        assignTime.put(job, Calendar.getInstance());
        storeJobAction(job.getId(), comm, MessageHeaders.JOB);
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
            storeJobAction(ssj.getId(), comm, JobMessageHeaders.JOB_CANCEL);
            if (isClientOnline(comm)) {
                log.log(Level.CONFIG, "Job with id {0} has been sent again to client with id {1} for confirmation.", new Object[]{ssj.getId(), comm.getId()});
                storeJobAction(ssj.getId(), comm, MessageHeaders.JOB);
                ssj.submitJob(comm);
            } else {
                log.log(Level.CONFIG, "Job with id {0} hasnt been accepted in time, so it was cancelled and returned to queue.", new Object[]{ssj.getId(), MAX_JOB_ASSIGN_TIME});
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
                        log.log(Level.CONFIG, "Client with id {0} is not reachable, its task has been cancelled and returned to queue.", comm.getId());
                        final List<ServerSideJob> reassignedList = jobsWaitingAssignment.get(comm);
                        for (ServerSideJob reassigned : reassignedList) {
                            reassigned.cancelJob();
                            storeJobAction(reassigned.getId(), comm, JobMessageHeaders.JOB_CANCEL);
                            if (!jobQueue.contains(reassigned)) {
                                jobQueue.addFirst(reassigned);
                            }
                        }
                        jobsWaitingAssignment.remove(comm);
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
    public void receiveData(final Identifiable data) {
        if (data instanceof Message) {
            final Message m = (Message) data;
            final UUID id = m.getId();
            ServerSideJob ssj;
            Entry<Communicator, List<ServerSideJob>> e;
            List<ServerSideJob> list;
            Iterator<ServerSideJob> itJob;
            Iterator<Entry<Communicator, List<ServerSideJob>>> itEntry;

            switch (m.getHeader()) {
                case JobMessageHeaders.JOB_ACCEPT:
                    loop:
                    {
                        itEntry = jobsWaitingAssignment.entrySet().iterator();
                        while (itEntry.hasNext()) {
                            e = itEntry.next();
                            list = e.getValue();
                            itJob = list.iterator();
                            while (itJob.hasNext()) {
                                ssj = itJob.next();
                                if (ssj.getId().equals(id)) {
                                    itJob.remove();
                                    if (list.isEmpty()) {
                                        itEntry.remove();
                                    }
                                    addJob(e.getKey(), ssj, jobComputing);
                                    log.log(Level.FINE, "Job with ID {0} has been accepted.", id);
                                    storeJobAction(ssj.getId(), e.getKey(), JobMessageHeaders.JOB_ACCEPT);
                                    break loop;
                                }
                            }
                        }
                        log.log(Level.WARNING, "JobAccept received for illegal UUID - {0}", id);
                    }
                    break;
                case JobMessageHeaders.JOB_CANCEL:
                    loop:
                    {
                        itEntry = jobComputing.entrySet().iterator();
                        while (itEntry.hasNext()) {
                            e = itEntry.next();
                            list = e.getValue();
                            itJob = list.iterator();
                            while (itJob.hasNext()) {
                                ssj = itJob.next();
                                if (ssj.getId().equals(id)) {
                                    itJob.remove();
                                    if (list.isEmpty()) {
                                        itEntry.remove();
                                    }
                                    jobQueue.addFirst(ssj);
                                    log.log(Level.CONFIG, "Job with ID {0} has been cancelled.", id);
                                    storeJobAction(ssj.getId(), e.getKey(), JobMessageHeaders.JOB_CANCEL);
                                    wakeUp();
                                    break loop;
                                }
                            }
                        }
                        log.log(Level.WARNING, "JobCancel received for illegal UUID - {0}", id);
                    }
                    break;
                case JobMessageHeaders.JOB_RESULT:
                    loop:
                    {
                        itEntry = jobComputing.entrySet().iterator();
                        while (itEntry.hasNext()) {
                            e = itEntry.next();
                            list = e.getValue();
                            itJob = list.iterator();
                            while (itJob.hasNext()) {
                                ssj = itJob.next();
                                if (ssj.getId().equals(id)) {
                                    itJob.remove();
                                    if (list.isEmpty()) {
                                        itEntry.remove();
                                    }
                                    log.log(Level.CONFIG, "Job with ID {0} has been computed succefully.", id);
                                    storeJobAction(ssj.getId(), e.getKey(), JobMessageHeaders.JOB_RESULT);
                                    wakeUp();
                                    break loop;
                                }
                            }
                        }
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

    private void storeJobAction(final UUID jobId, final Communicator owner, final String action) {
        List<JobAction> l = jobHistory.get(jobId);
        if (l == null) {
            l = new LinkedList<>();
            jobHistory.put(jobId, l);
        }
        l.add(new JobAction(jobId, owner, action));
    }
}
