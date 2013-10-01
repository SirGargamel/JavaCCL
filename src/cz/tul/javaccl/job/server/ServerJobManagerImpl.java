package cz.tul.javaccl.job.server;

import cz.tul.javaccl.GenericResponses;
import cz.tul.javaccl.IService;
import cz.tul.javaccl.communicator.Communicator;
import cz.tul.javaccl.exceptions.ConnectionException;
import cz.tul.javaccl.exceptions.ConnectionExceptionCause;
import cz.tul.javaccl.job.JobCount;
import cz.tul.javaccl.job.JobMessageHeaders;
import cz.tul.javaccl.job.JobStatus;
import cz.tul.javaccl.job.JobTask;
import cz.tul.javaccl.messaging.Message;
import cz.tul.javaccl.server.ClientManager;
import cz.tul.javaccl.server.DataStorage;
import cz.tul.javaccl.socket.ListenerRegistrator;
import cz.tul.javaccl.messaging.Identifiable;
import cz.tul.javaccl.socket.Listener;
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
    private static final int JOB_TIMEOUT = 10_000;
    private static final int JOB_CANCEL_WAIT_TIME = 2_000;
    private final ClientManager clientManager;
    private DataStorage dataStorage;
    private final ListenerRegistrator listenerRegistrator;
    private final Deque<ServerSideJob> jobQueue;
    private final Map<Communicator, Calendar> lastTimeOnline;
    private final Map<Communicator, Integer> jobCount;
    private final Set<JobRecord> activeJobs;
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
        lastTimeOnline = new ConcurrentHashMap<>();
        jobQueue = new ConcurrentLinkedDeque<>();
        allJobs = new LinkedList<>();
        allJobsOuter = Collections.unmodifiableCollection(allJobs);
        jobHistory = new HashMap<>();
        activeJobs = new HashSet<>();
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
    public void stopAllJobs() {
        for (ServerSideJob ssj : jobQueue) {
            ssj.setResult(null);
            ssj.setStatus(JobStatus.CANCELED);
        }
        
        Collection<JobRecord> jobs = new ArrayList<>(activeJobs);
        for (JobRecord jr : jobs) {
            try {
                jr.getJob().cancelJob();
            } catch (ConnectionException ex) {
                log.log(Level.WARNING, "Could not contact client {0} for job cancelation.", jr.getOwner().getTargetId());
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
        long dif;
        ServerSideJob j;
        JobRecord jr;
        final Collection<JobRecord> remove = new LinkedList<>();
        synchronized (activeJobs) {
            Iterator<JobRecord> it = activeJobs.iterator();
            while (it.hasNext()) {
                jr = it.next();
                j = jr.getJob();
                if (j.getStatus().equals(JobStatus.SENT)) {
                    dif = Calendar.getInstance().getTimeInMillis() - jr.getLastAction().getTimeInMillis();
                    if (dif > JOB_TIMEOUT) {
                        try {
                            j.cancelJob();
                            j.setStatus(JobStatus.SUBMITTED);
                        } catch (ConnectionException ex) {
                            log.log(Level.WARNING, "Could not contact client with ID {0} for job cancelation.", jr.getOwner().getTargetId());
                        }
                        storeJobAction(j, jr.getOwner().getTargetId(), JobMessageHeaders.JOB_CANCEL);
                        log.log(Level.CONFIG, "Job with id {0} hasnt been accepted in time, so it was cancelled and returned to queue.", new Object[]{j.getId()});
                        if (!jobQueue.contains(j)) {
                            jobQueue.addFirst(j);
                        }
                        remove.add(jr);
                    }
                }
            }
            activeJobs.removeAll(remove);
        }
    }
    
    private void checkClientStatuses() {
        long dif;
        ServerSideJob j;
        JobRecord jr;
        Calendar lastOnline;
        Communicator comm;
        final Collection<JobRecord> remove = new LinkedList<>();
        synchronized (activeJobs) {
            Iterator<JobRecord> it = activeJobs.iterator();
            while (it.hasNext()) {
                jr = it.next();
                j = jr.getJob();
                if (j.getStatus().equals(JobStatus.ACCEPTED)) {
                    comm = jr.getOwner();
                    if (!isClientOnline(comm)) {
                        lastOnline = lastTimeOnline.get(comm);
                        if (lastOnline == null) {
                            lastOnline = jr.getLastAction();
                        }
                        
                        dif = Calendar.getInstance().getTimeInMillis() - lastOnline.getTimeInMillis();
                        if (dif > JOB_TIMEOUT) {
                            try {
                                j.cancelJob();
                                j.setStatus(JobStatus.SUBMITTED);
                            } catch (ConnectionException ex) {
                                log.log(Level.WARNING, "Could not contact client with ID {0} for job cancelation.", jr.getOwner().getTargetId());
                            }
                            storeJobAction(j, jr.getOwner().getTargetId(), JobMessageHeaders.JOB_CANCEL);
                            log.log(Level.CONFIG, "Jobs (id {0}) owner went offline, so it was cancelled and returned to queue.", new Object[]{j.getId()});
                            if (!jobQueue.contains(j)) {
                                jobQueue.addFirst(j);
                            }
                            remove.add(jr);
                        }
                    }
                }
            }
            activeJobs.removeAll(remove);
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
    
    private boolean assignJob(final ServerSideJob job, final Communicator comm) {
        boolean result;
        JobRecord jr = null;
        try {
            listenerRegistrator.setIdListener(job.getId(), this);
            jr = new JobRecord(job);
            jr.setOwner(comm);
            activeJobs.add(jr);
            submitJob(job, comm);
            job.setStatus(JobStatus.SENT);
            storeJobAction(job, comm.getTargetId(), JobMessageHeaders.JOB_TASK);
            result = true;
        } catch (ConnectionException ex) {
            activeJobs.remove(jr);
            job.setStatus(JobStatus.SUBMITTED);
            storeJobAction(job, null, JobMessageHeaders.JOB_CANCEL);
            return false;
        }
        
        return result;
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
        Communicator comm2;
        synchronized (activeJobs) {
            for (JobRecord jr : activeJobs) {
                comm2 = jr.getOwner();
                if (comm2 != null && comm2.equals(comm)) {
                    count++;
                }
            }
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
        final boolean result = comm.isOnline();
        if (result) {
            storeClientOnlineStatus(comm);
        }
        return result;
    }
    
    private void storeClientOnlineStatus(final Communicator comm) {
        lastTimeOnline.put(comm, Calendar.getInstance());
    }
    
    private void submitJob(final ServerSideJob ssj, final Communicator comm) throws ConnectionException {
        final JobTask jt = new JobTask(ssj.getId(), JobMessageHeaders.JOB_TASK, ssj.getTask());
        final Object response = comm.sendData(jt);
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
    
    private Object acceptJob(final UUID id) {
        ServerSideJob ssj;
        synchronized (activeJobs) {
            for (JobRecord jr : activeJobs) {
                ssj = jr.getJob();
                if (ssj.getId().equals(id)) {
                    ssj.setStatus(JobStatus.ACCEPTED);
                    jr.updateTime();
                    log.log(Level.CONFIG, "Job with ID {0} has been accepted.", id);
                    storeJobAction(ssj, jr.getOwner().getTargetId(), JobMessageHeaders.JOB_ACCEPT);
                    return GenericResponses.OK;
                }
            }
        }
        log.log(Level.WARNING, "JobAccept received for illegal UUID - {0}", id);
        return GenericResponses.UUID_UNKNOWN;
    }
    
    private Object cancelJobByClient(final UUID id) {
        ServerSideJob j;
        synchronized (activeJobs) {
            final Iterator<JobRecord> it = activeJobs.iterator();
            JobRecord jr;
            while (it.hasNext()) {
                jr = it.next();
                j = jr.getJob();
                if (j.getId().equals(id)) {
                    jobQueue.addFirst(j);
                    j.setStatus(JobStatus.SUBMITTED);
                    it.remove();
                    log.log(Level.CONFIG, "Job with ID {0} has been cancelled.", id);
                    storeJobAction(j, jr.getOwner().getTargetId(), JobMessageHeaders.JOB_CANCEL);
                    wakeUp();
                    return GenericResponses.OK;
                }
            }
        }
        log.log(Level.WARNING, "JobCancel received for illegal UUID - {0}", id);
        return GenericResponses.UUID_UNKNOWN;
    }
    
    private Object finishJob(final JobTask jt) {
        final UUID id = jt.getJobId();
        ServerSideJob j;
        synchronized (activeJobs) {
            final Iterator<JobRecord> it = activeJobs.iterator();
            JobRecord jr;
            while (it.hasNext()) {
                jr = it.next();
                j = jr.getJob();
                if (j.getId().equals(id)) {
                    j.setResult(jt.getTask());
                    it.remove();
                    log.log(Level.CONFIG, "Job with ID {0} has been computed succefully.", id);
                    storeJobAction(j, jr.getOwner().getTargetId(), JobMessageHeaders.JOB_RESULT);
                    wakeUp();
                    return GenericResponses.OK;
                }
            }
        }
        log.log(Level.WARNING, "JobCancel received for illegal UUID - {0}", id);
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
    public void cancelJobByServer(ServerSideJob job) throws ConnectionException {
        synchronized (activeJobs) {
            final Iterator<JobRecord> it = activeJobs.iterator();
            JobRecord jr;
            ServerSideJob j;
            while (it.hasNext()) {
                jr = it.next();
                j = jr.getJob();
                if (job == j) {
                    JobTask jt = new JobTask(job.getId(), JobMessageHeaders.JOB_CANCEL, null);
                    jr.getOwner().sendData(jt);
                    job.setStatus(JobStatus.CANCELED);
                    jr.updateTime();
                    it.remove();
                    log.log(Level.CONFIG, "Job with ID {0} has been cancelled by server.", j.getId());
                }
            }
        }
    }
}
