package cz.tul.comm.job.server;

import cz.tul.comm.GenericResponses;
import cz.tul.comm.IService;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.communicator.Status;
import cz.tul.comm.exceptions.ConnectionException;
import cz.tul.comm.exceptions.ConnectionExceptionCause;
import cz.tul.comm.job.JobCount;
import cz.tul.comm.job.JobMessageHeaders;
import cz.tul.comm.job.JobStatus;
import static cz.tul.comm.job.JobStatus.ACCEPTED;
import static cz.tul.comm.job.JobStatus.SENT;
import cz.tul.comm.job.JobTask;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.server.ClientManager;
import cz.tul.comm.server.DataStorage;
import cz.tul.comm.socket.ListenerRegistrator;
import cz.tul.comm.socket.queue.Identifiable;
import cz.tul.comm.socket.queue.Listener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
public class ServerJobManagerImpl extends Thread implements IService, Listener<Identifiable>, ServerJobManager, JobCancelManager {

    private static final Logger log = Logger.getLogger(ServerJobManagerImpl.class.getName());
    private static final int WAIT_TIME = 1_000;
    private static final int MAX_CLIENT_NA_TIME = 5_000;
    private static final int MAX_JOB_ASSIGN_TIME = 5_000;
    private static final int JOB_CANCEL_WAIT_TIME = 2_000;
    private final ClientManager clientManager;
    private DataStorage dataStorage;
    private final ListenerRegistrator listenerRegistrator;
    private final Deque<ServerSideJob> jobQueue;
    private final Map<Communicator, List<AssignmentRecord>> jobsWaitingAssignment;
    private final Map<Communicator, List<ServerSideJob>> jobComputing;
    private final Map<Communicator, Calendar> lastTimeOnline;
    private final Map<Communicator, Integer> jobCount;
    private final Map<ServerSideJob, Communicator> owners;
    private final Collection<Job> allJobs;
    private final Collection<Job> allJobsOuter;
    private final Map<Job, List<JobAction>> jobHistory;
    private boolean run;

    /**
     * Prepare new instance.
     *
     * @param clientManager client manager
     * @param listenerRegistrator listener registrator
     */
    public ServerJobManagerImpl(final ClientManager clientManager, final ListenerRegistrator listenerRegistrator) {
        this.clientManager = clientManager;
        this.listenerRegistrator = listenerRegistrator;
        jobsWaitingAssignment = new ConcurrentHashMap<>();
        jobComputing = new ConcurrentHashMap<>();
        lastTimeOnline = new ConcurrentHashMap<>();
        jobQueue = new ConcurrentLinkedDeque<>();
        allJobs = new LinkedList<>();
        allJobsOuter = Collections.unmodifiableCollection(allJobs);
        jobHistory = new HashMap<>();
        owners = new HashMap<>();
        jobCount = new HashMap<>();

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
    public Job submitJob(final Object task) throws IllegalArgumentException {
        final ServerSideJob result = new ServerSideJob(task, this);
        jobQueue.add(result);
        allJobs.add(result);

        log.log(Level.CONFIG, "Job with ID {0} submitted.", result.getId());
        wakeUp();

        return result;
    }

    @Override
    public void waitForAllJobs() {
        while (!allJobsDone()) {
            synchronized (this) {
                try {
                    this.wait(WAIT_TIME);
                } catch (InterruptedException ex) {
                    log.log(Level.WARNING, "Waiting for all jobs to complete failed.", ex);
                }
            }
        }
    }

    private boolean allJobsDone() {
        boolean result = true;

        for (Job j : allJobs) {
            if (!j.isDone()) {
                result = false;
                break;
            }
        }

        return result;
    }

    @Override
    public synchronized void stopAllJobs() {
        jobQueue.clear();

        for (Job j : allJobs) {
            try {
                j.cancelJob();
            } catch (ConnectionException ex) {
                log.log(Level.WARNING, "Could not contact client with ID {0} for job cancelation.", owners.get(j).getTargetId());
            }
        }

        lastTimeOnline.clear();
    }

    @Override
    public Collection<Job> getAllJobs() {
        return allJobsOuter;
    }

    @Override
    public void run() {
        log.fine("JobManager has been started.");
        while (run) {
            checkAssignedJobs();
            checkClientStatuses();
            if (!jobQueue.isEmpty()) {
                assignJobs();
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

    private void checkAssignedJobs() {
        final long time = Calendar.getInstance().getTimeInMillis();
        final Set<ServerSideJob> reassign = new HashSet<>();

        long dif;
        Iterator<AssignmentRecord> it;
        AssignmentRecord ar;
        for (Entry<Communicator, List<AssignmentRecord>> e : jobsWaitingAssignment.entrySet()) {
            it = e.getValue().iterator();
            while (it.hasNext()) {
                ar = it.next();
                dif = time - ar.getAssignTime().getTimeInMillis();
                if (dif > MAX_JOB_ASSIGN_TIME) {
                    reassign.add(ar.getJob());
                    it.remove();
                }
            }
        }

        Communicator comm;
        for (ServerSideJob ssj : reassign) {
            // check client, then resend or give to another
            comm = owners.get(ssj);
            try {
                ssj.cancelJob();
                ssj.setStatus(JobStatus.SUBMITTED);
            } catch (ConnectionException ex) {
                log.log(Level.WARNING, "Could not contact client with ID {0} for job cancelation.", comm.getTargetId());
            }
            storeJobAction(ssj, comm.getTargetId(), JobMessageHeaders.JOB_CANCEL);

            log.log(Level.CONFIG, "Job with id {0} hasnt been accepted in time, so it was cancelled and returned to queue.", new Object[]{ssj.getId(), MAX_JOB_ASSIGN_TIME});
            jobsWaitingAssignment.remove(comm);
            if (!jobQueue.contains(ssj)) {
                jobQueue.addFirst(ssj);
            }
        }
    }

    private void checkClientStatuses() {
        ServerSideJob ssj;

        for (Communicator comm : jobsWaitingAssignment.keySet()) {
            if (!isClientOnline(comm)) {
                final Calendar actualTime = Calendar.getInstance();
                final Calendar lastOnline = lastTimeOnline.get(comm);
                if (lastOnline != null) {
                    final long dif = actualTime.getTimeInMillis() - lastOnline.getTimeInMillis();
                    if (dif > MAX_CLIENT_NA_TIME) {
                        log.log(Level.CONFIG, "Client with id {0} is not reachable, its task has been cancelled and returned to queue.", comm.getTargetId());
                        final List<AssignmentRecord> reassignedList = jobsWaitingAssignment.get(comm);
                        for (AssignmentRecord reassigned : reassignedList) {
                            ssj = reassigned.getJob();
                            try {
                                ssj.cancelJob();
                                ssj.setStatus(JobStatus.SUBMITTED);
                            } catch (ConnectionException ex) {
                                log.log(Level.WARNING, "Could not contact client with ID {0} for job cancelation.", comm.getTargetId());
                            }
                            storeJobAction(ssj, comm.getTargetId(), JobMessageHeaders.JOB_CANCEL);
                            if (!jobQueue.contains(ssj)) {
                                jobQueue.addFirst(ssj);
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

    private void assignJobs() {
        ServerSideJob job;
        final Collection<Communicator> clients = clientManager.getClients();
        final Collection<ServerSideJob> putBack = new LinkedList<>();
        Communicator comm;
        while (!jobQueue.isEmpty() && isAnyClientFree(clients)) {
            job = jobQueue.poll();
            comm = pickClient(job, clients);
            if (comm != null && assignJob(job, comm)) {
                log.log(Level.CONFIG, "Job with ID {0} assigned to client with ID {1}.", new Object[]{job.getId(), comm.getTargetId()});
            } else {
                putBack.add(job);
                if (comm != null) {
                    log.log(Level.WARNING, "Failed to assign job to client with ID {0}", comm.getTargetId());
                } else {
                    log.log(Level.WARNING, "Failed to pick client even after checking if any client is aviable.");
                }
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
        boolean result = false;
        if (isClientOnline(comm)) {
            int count = countActiveJobs(comm);

            final int maxCount;
            if (jobCount.containsKey(comm)) {
                maxCount = jobCount.get(comm);
            } else {
                maxCount = 1;
            }

            return (count < maxCount);
        }

        return result;
    }

    private int countActiveJobs(final Communicator comm) {
        int count = 0;
        final List<AssignmentRecord> jobsWaiting = jobsWaitingAssignment.get(comm);
        if (jobsWaiting != null) {
            count += jobsWaiting.size();
        }
        final List<ServerSideJob> jobsComputing = jobComputing.get(comm);
        if (jobsComputing != null) {
            count += jobsComputing.size();
        }
        return count;
    }

    private Communicator pickClient(final Job job, final Collection<Communicator> clients) {
        final List<Communicator> candidates = new LinkedList<>();
        // pick online only
        for (Communicator comm : clients) {
            if (isClientFree(comm)) {
                candidates.add(comm);
            }
        }
        Collections.sort(candidates, new Comparator<Communicator>() {
            @Override
            public int compare(Communicator o1, Communicator o2) {
                return (countActiveJobs(o1) - countActiveJobs(o2));
            }
        });
        // 1st round - no actions with job before
        List<JobAction> actionList = jobHistory.get(job);
        for (Communicator comm : candidates) {
            if (isCommPresent(actionList, comm)) {
                continue;
            }
            return comm;
        }
        // 2nd round - longest time from last cancel
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
                log.log(Level.FINE, "No communicator assigned for job {0}, too soon after last job cancel", job.getId());
                return null;
            }
        } else {
            log.log(Level.FINE, "No communicator assigned for job {0}, no aviable found.", job.getId());
            return null;
        }
    }

    private boolean isCommPresent(final List<JobAction> actionList, final Communicator comm) {
        if (actionList == null || comm == null) {
            return false;
        }

        for (JobAction ja : actionList) {
            if (comm.getTargetId().equals(ja.getOwnerId())) {
                return true;
            }
        }
        return false;
    }

    private Calendar lastCancelTime(final List<JobAction> actionList, final Communicator comm) {
        List<Calendar> cancelTimes = new ArrayList<>();
        for (JobAction ja : actionList) {
            if (ja.getOwnerId().equals(comm.getTargetId()) && ja.getActionDescription().equals(JobMessageHeaders.JOB_CANCEL)) {
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
        final boolean result = !s.equals(Status.OFFLINE);
        if (result) {
            storeClientOnlineStatus(comm);
        }
        return result;
    }

    private void storeClientOnlineStatus(final Communicator comm) {
        lastTimeOnline.put(comm, Calendar.getInstance());
    }

    private synchronized boolean assignJob(final ServerSideJob job, final Communicator comm) {
        boolean result;
        try {
            listenerRegistrator.setIdListener(job.getId(), this);
            List<AssignmentRecord> l = jobsWaitingAssignment.get(comm);
            if (l == null) {
                l = new ArrayList<>(1);
                jobsWaitingAssignment.put(comm, l);
            }
            l.add(new AssignmentRecord(job, Calendar.getInstance(Locale.getDefault())));
            submitJob(job, comm);
            job.setStatus(JobStatus.SENT);
            storeJobAction(job, comm.getTargetId(), JobMessageHeaders.JOB_TASK);
            result = true;
        } catch (ConnectionException ex) {
            List<AssignmentRecord> l = jobsWaitingAssignment.get(comm);
            if (l != null) {
                final Iterator<AssignmentRecord> it = l.iterator();
                while (it.hasNext()) {
                    if (it.next().getJob() == job) {
                        it.remove();
                    }
                }
            }
            job.setStatus(JobStatus.SUBMITTED);
            storeJobAction(job, null, JobMessageHeaders.JOB_TASK);
            return false;
        }

        return result;
    }

    private void submitJob(final ServerSideJob ssj, final Communicator comm) throws ConnectionException {
        owners.put(ssj, comm);
        final JobTask jt = new JobTask(ssj.getId(), JobMessageHeaders.JOB_TASK, ssj.getTask());
        final Object response;
        response = comm.sendData(jt);
        if (GenericResponses.OK.equals(response)) {
            ssj.setStatus(JobStatus.SENT);
        } else {
            throw new ConnectionException(ConnectionExceptionCause.UNKNOWN);
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
    public Object receiveData(final Identifiable data) {
        if (data instanceof JobTask) {
            final JobTask jt = (JobTask) data;
            final UUID id = jt.getJobId();

            switch (jt.getTaskDescription()) {
                case JobMessageHeaders.JOB_ACCEPT:
                    return acceptJob(id);
                case JobMessageHeaders.JOB_CANCEL:
                    return cancelJobByClient(id);
                case JobMessageHeaders.JOB_RESULT:
                    return finishJob(jt);
                case JobMessageHeaders.JOB_DATA_REQUEST:
                    return dataStorage.requestData(jt.getTask());
                default:
                    return GenericResponses.ILLEGAL_HEADER;
            }
        } else if (data instanceof Message) {
            final Message m = (Message) data;

            switch (m.getHeader()) {
                case JobMessageHeaders.JOB_COUNT:
                    Object count = m.getData();
                    if (count instanceof JobCount) {
                        JobCount jc = (JobCount) count;
                        jobCount.put(clientManager.getClient(jc.getClientId()), jc.getJobCount());
                    }
                    return GenericResponses.OK;
                default:
                    return GenericResponses.ILLEGAL_HEADER;
            }
        } else {
            return GenericResponses.ILLEGAL_DATA;
        }
    }

    private synchronized Object acceptJob(final UUID id) {
        ServerSideJob ssj;
        Entry<Communicator, List<AssignmentRecord>> e;
        List<AssignmentRecord> list;
        Iterator<AssignmentRecord> itJob;
        final Iterator<Entry<Communicator, List<AssignmentRecord>>> itEntry;
        itEntry = jobsWaitingAssignment.entrySet().iterator();
        while (itEntry.hasNext()) {
            e = itEntry.next();
            list = e.getValue();
            itJob = list.iterator();
            while (itJob.hasNext()) {
                ssj = itJob.next().getJob();
                if (ssj.getId().equals(id)) {
                    itJob.remove();
                    if (list.isEmpty()) {
                        itEntry.remove();
                    }

                    List<ServerSideJob> l = jobComputing.get(e.getKey());
                    if (l == null) {
                        l = new ArrayList<>(1);
                        jobComputing.put(e.getKey(), l);
                    }
                    l.add(ssj);

                    ssj.setStatus(JobStatus.ACCEPTED);
                    log.log(Level.CONFIG, "Job with ID {0} has been accepted.", id);
                    storeJobAction(ssj, e.getKey().getTargetId(), JobMessageHeaders.JOB_ACCEPT);
                    return GenericResponses.OK;
                }
            }
        }
        log.log(Level.WARNING, "JobAccept received for illegal UUID - {0}", id);
        return GenericResponses.UUID_UNKNOWN;
    }

    private synchronized Object cancelJobByClient(final UUID id) {
        ServerSideJob job = findJob(id);
        if (jobCleanUp(job)) {
            owners.remove(job);
            jobQueue.addFirst(job);
            job.setStatus(JobStatus.SUBMITTED);
            log.log(Level.CONFIG, "Job with ID {0} has been cancelled.", id);
            wakeUp();
            return GenericResponses.OK;
        } else {
            return GenericResponses.UUID_UNKNOWN;
        }
    }

    private synchronized Object finishJob(final JobTask jt) {
        final UUID id = jt.getJobId();

        ServerSideJob ssj;
        Entry<Communicator, List<ServerSideJob>> e;
        List<ServerSideJob> list;
        Iterator<ServerSideJob> itJob;
        final Iterator<Entry<Communicator, List<ServerSideJob>>> itEntry;
        itEntry = jobComputing.entrySet().iterator();
        while (itEntry.hasNext()) {
            e = itEntry.next();
            list = e.getValue();
            itJob = list.iterator();
            while (itJob.hasNext()) {
                ssj = itJob.next();
                if (ssj.getId().equals(id)) {
                    synchronized (jobComputing) {
                        itJob.remove();
                        if (list.isEmpty()) {
                            itEntry.remove();
                        }
                    }
                    ssj.setResult(jt.getTask());
                    log.log(Level.CONFIG, "Job with ID {0} has been computed succefully.", id);
                    storeJobAction(ssj, e.getKey().getTargetId(), JobMessageHeaders.JOB_RESULT);
                    wakeUp();
                    return GenericResponses.OK;
                }
            }
        }
        log.log(Level.WARNING, "JobResult received for illegal UUID - {0}", id);
        return GenericResponses.UUID_UNKNOWN;
    }

    private void wakeUp() {
        synchronized (this) {
            this.notify();
        }
    }

    private void storeJobAction(final Job job, final UUID ownerId, final String action) {
        List<JobAction> l = jobHistory.get(job);
        if (l == null) {
            l = new LinkedList<>();
            jobHistory.put(job, l);
        }
        l.add(new JobAction(job, ownerId, action));
    }

    @Override
    public synchronized void cancelJobByServer(ServerSideJob job) throws ConnectionException {
        if (jobCleanUp(job)) {
            JobTask jt = new JobTask(job.getId(), JobMessageHeaders.JOB_CANCEL, null);
            owners.get(job).sendData(jt);
            owners.remove(job);
        }
        job.setStatus(JobStatus.CANCELED);     
    }

    private synchronized boolean jobCleanUp(ServerSideJob job) {
        boolean result = false;
        if (job != null) {
            JobStatus s = job.getStatus();
            if (s == JobStatus.SENT || s == JobStatus.ACCEPTED) {
                Communicator comm = owners.get(job);
                if (comm != null) {                    
                    storeJobAction(job, comm.getTargetId(), JobMessageHeaders.JOB_CANCEL);

                    switch (s) {
                        case SENT:
                            final Iterator<AssignmentRecord> it = jobsWaitingAssignment.get(comm).iterator();
                            AssignmentRecord ar;
                            while (it.hasNext()) {
                                ar = it.next();
                                if (job.equals(ar.getJob())) {
                                    it.remove();
                                    result = true;
                                    break;
                                }
                            }
                            break;
                        case ACCEPTED:
                            jobComputing.get(comm).remove(job);
                            result = true;
                            break;
                    }
                }
            }
        }
        return result;
    }

    private ServerSideJob findJob(final UUID jobId) {
        Job result = null;
        if (jobId != null) {
            for (Job j : allJobs) {
                if (jobId.equals(j.getId())) {
                    result = j;
                    break;
                }
            }
        }
        return (ServerSideJob) result;
    }

    private static class AssignmentRecord {

        private ServerSideJob ssj;
        private Calendar assignTime;

        public AssignmentRecord(ServerSideJob ssj, Calendar assignTime) {
            this.ssj = ssj;
            this.assignTime = assignTime;
        }

        public ServerSideJob getJob() {
            return ssj;
        }

        public Calendar getAssignTime() {
            return assignTime;
        }
    }
}
