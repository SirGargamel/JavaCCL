package cz.tul.javaccl.job.server;

import cz.tul.javaccl.Constants;
import cz.tul.javaccl.GenericResponses;
import cz.tul.javaccl.IService;
import cz.tul.javaccl.communicator.Communicator;
import cz.tul.javaccl.exceptions.ConnectionException;
import cz.tul.javaccl.exceptions.ConnectionExceptionCause;
import cz.tul.javaccl.job.ClientJobSettings;
import cz.tul.javaccl.job.JobConstants;
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
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class managing job assignment and management.
 *
 * @author Petr Ječmen
 */
public class ServerJobManagerImpl extends Thread implements IService, Listener<Identifiable>, ServerJobManager, JobCancelManager {

    private static final Logger log = Logger.getLogger(ServerJobManagerImpl.class.getName());
    private final ClientManager clientManager;
    private DataStorage dataStorage;
    private final ListenerRegistrator listenerRegistrator;
    private final Deque<ServerSideJob> jobQueue;
    private final Map<Communicator, Calendar> lastTimeOnline;
    private final Map<Communicator, Integer> jobCount;
    private final Map<Communicator, Integer> jobComplexity;
    private final Set<JobRecord> activeJobs;
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
        lastTimeOnline = new ConcurrentHashMap<Communicator, Calendar>();
        jobQueue = new LinkedBlockingDeque<ServerSideJob>();
        jobHistory = new HashMap<Job, List<JobAction>>();
        activeJobs = new HashSet<JobRecord>();
        jobCount = new HashMap<Communicator, Integer>();
        jobComplexity = new HashMap<Communicator, Integer>();

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
        return submitJob(task, JobConstants.DEFAULT_COMPLEXITY);
    }

    @Override
    public Job submitJob(Object task, int complexity) throws IllegalArgumentException {
        final ServerSideJob result = new ServerSideJob(task, this, complexity);
        jobQueue.add(result);

        log.log(Level.FINE, "Job with ID " + result.getId() + " and complexity " + complexity + " submitted.");
        wakeUp();

        return result;
    }

    @Override
    public void waitForAllJobs() {
        while (!activeJobs.isEmpty()) {
            synchronized (this) {
                try {
                    this.wait(Constants.DEFAULT_TIMEOUT);
                } catch (InterruptedException ex) {
                    log.log(Level.WARNING, "Waiting for all jobs to complete failed.");
                    log.log(Level.FINE, "Waiting for all jobs to complete failed.", ex);
                }
            }
        }
    }

    @Override
    public void stopAllJobs() {
        for (ServerSideJob ssj : jobQueue) {
            ssj.setResult(null);
            ssj.setStatus(JobStatus.CANCELED);
        }

        Collection<JobRecord> jobs = new ArrayList<JobRecord>(activeJobs);
        for (JobRecord jr : jobs) {
            try {
                jr.getJob().cancelJob();
            } catch (ConnectionException ex) {
                log.log(Level.WARNING, "Could not contact client " + jr.getOwner().getTargetId() + " for job cancelation.");
            }

        }

        lastTimeOnline.clear();
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
                    this.wait(Constants.DEFAULT_TIMEOUT);
                } catch (InterruptedException ex) {
                    log.log(Level.WARNING, "Waiting of JobManager has been interrupted.");
                    log.log(Level.FINE, "Waiting of JobManager has been interrupted.", ex);
                }
            }
        }
    }

    private void checkAssignedJobs() {
        long dif;
        ServerSideJob j;
        JobRecord jr;
        final Collection<JobRecord> remove = new LinkedList<JobRecord>();
        synchronized (activeJobs) {
            Iterator<JobRecord> it = activeJobs.iterator();
            while (it.hasNext()) {
                jr = it.next();
                j = jr.getJob();
                if (j.getStatus().equals(JobStatus.SENT)) {
                    dif = Calendar.getInstance().getTimeInMillis() - jr.getLastAction().getTimeInMillis();
                    if (dif > Constants.DEFAULT_TIMEOUT) {
                        try {
                            j.cancelJob();
                            j.setStatus(JobStatus.SUBMITTED);
                        } catch (ConnectionException ex) {
                            log.log(Level.WARNING, "Could not contact client with ID " + jr.getOwner().getTargetId() + " for job cancelation.");
                        }
                        storeJobAction(j, jr.getOwner().getTargetId(), JobConstants.JOB_CANCEL);
                        log.log(Level.INFO, "Job with id " + j.getId() + " hasnt been accepted in time, so it was cancelled and returned to queue.");
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
        final Collection<JobRecord> remove = new LinkedList<JobRecord>();
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
                        if (dif > Constants.DEFAULT_TIMEOUT) {
                            try {
                                j.cancelJob();
                                j.setStatus(JobStatus.SUBMITTED);
                            } catch (ConnectionException ex) {
                                log.log(Level.WARNING, "Could not contact client with ID " + jr.getOwner().getTargetId() + " for job cancelation.");
                            }
                            storeJobAction(j, jr.getOwner().getTargetId(), JobConstants.JOB_CANCEL);
                            log.log(Level.INFO, "Jobs (id " + j.getId() + ") owner went offline, so it was cancelled and returned to queue.");
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
        Collection<Communicator> comms;
        while (!jobQueue.isEmpty() && isAnyClientFree(clients)) {
            comms = pickOnlineClients(clients);

            for (Communicator comm : comms) {
                job = pickJob(comm);
                if (job != null) {
                    if (assignJob(job, comm)) {
                        log.log(Level.INFO, "Job with ID " + job.getId() + " assigned to client with ID " + comm.getTargetId());
                        jobQueue.remove(job);
                    } else {
                        log.log(Level.WARNING, "Failed to assign job with id " + job.getId() + " to client with ID " + comm.getTargetId());
                    }
                } else {
                    log.fine("No job available for client with ID " + comm.getTargetId());
                }
            }
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
            storeJobAction(job, comm.getTargetId(), JobConstants.JOB_TASK);
            result = true;
        } catch (ConnectionException ex) {
            activeJobs.remove(jr);
            job.setStatus(JobStatus.SUBMITTED);
            storeJobAction(job, null, JobConstants.JOB_CANCEL);
            result = false;
        }

        return result;
    }

    private Collection<Communicator> pickOnlineClients(final Collection<Communicator> clients) {
        Collection<Communicator> result = new LinkedList<Communicator>();
        // pick online only
        for (Communicator comm : clients) {
            if (isClientFree(comm)) {
                result.add(comm);
            }
        }

        return result;
    }

    private ServerSideJob pickJob(final Communicator comm) {
        ServerSideJob result = null;

        // traverse job, check complexity and cancel time
        List<JobAction> actionList;
        Calendar cal;
        for (ServerSideJob job : jobQueue) {
            final int maxJobComplexity;
            if (jobComplexity.containsKey(comm)) {
                maxJobComplexity = jobComplexity.get(comm);
            } else {
                maxJobComplexity = JobConstants.DEFAULT_COMPLEXITY;
            }
            if (job.getComplexity() > maxJobComplexity) {
                continue;
            }

            actionList = jobHistory.get(job);
            if (actionList != null) {
                cal = lastCancelTime(actionList, comm);
                if (cal != null) {
                    if ((Calendar.getInstance(Locale.getDefault()).getTimeInMillis() - cal.getTimeInMillis()) > Constants.DEFAULT_TIMEOUT) {
                        result = job;
                        break;
                    }
                } else {
                    result = job;
                    break;
                }
            } else {
                result = job;
                break;
            }
        }

        return result;
    }

    private Calendar lastCancelTime(final List<JobAction> actionList, final Communicator comm) {
        List<Calendar> cancelTimes = new ArrayList<Calendar>();
        for (JobAction ja : actionList) {
            if (ja.getOwnerId().equals(comm.getTargetId()) && ja.getActionDescription().equals(JobConstants.JOB_CANCEL)) {
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
        final JobTask jt = new JobTask(ssj.getId(), JobConstants.JOB_TASK, ssj.getTask());
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

            final String descr = jt.getTaskDescription();
            if (descr != null) {
                if (descr.equals(JobConstants.JOB_ACCEPT)) {
                    return acceptJob(id);
                } else if (descr.equals(JobConstants.JOB_CANCEL)) {
                    return cancelJobByClient(id);
                } else if (descr.equals(JobConstants.JOB_RESULT)) {
                    return finishJob(jt);
                } else if (descr.equals(JobConstants.JOB_DATA_REQUEST)) {
                    return dataStorage.requestData(jt.getTask());
                } else {
                    return GenericResponses.ILLEGAL_HEADER;
                }
            } else {
                return GenericResponses.ILLEGAL_HEADER;
            }
        } else if (data instanceof Message) {
            final Message m = (Message) data;

            final String header = m.getHeader();
            if (header != null) {
                if (header.equals(JobConstants.JOB_COUNT)) {
                    Object count = m.getData();
                    if (count instanceof ClientJobSettings) {
                        ClientJobSettings jc = (ClientJobSettings) count;
                        jobCount.put(clientManager.getClient(jc.getClientId()), jc.getValue());
                    }
                    return GenericResponses.OK;
                }
                if (header.equals(JobConstants.JOB_COMPLEXITY)) {
                    Object count = m.getData();
                    if (count instanceof ClientJobSettings) {
                        ClientJobSettings jc = (ClientJobSettings) count;
                        jobComplexity.put(clientManager.getClient(jc.getClientId()), jc.getValue());
                    }
                    return GenericResponses.OK;
                } else {
                    return GenericResponses.ILLEGAL_HEADER;
                }
            } else {
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
                    log.log(Level.INFO, "Job with ID " + id + " has been accepted.");
                    storeJobAction(ssj, jr.getOwner().getTargetId(), JobConstants.JOB_ACCEPT);
                    return GenericResponses.OK;
                }
            }
        }
        log.log(Level.WARNING, "JobAccept received for illegal UUID - " + id);
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
                    log.log(Level.INFO, "Job with ID " + id + " has been cancelled.");
                    storeJobAction(j, jr.getOwner().getTargetId(), JobConstants.JOB_CANCEL);
                    wakeUp();
                    return GenericResponses.OK;
                }
            }
        }
        log.log(Level.WARNING, "JobCancel received for illegal UUID - " + id);
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
                    jobHistory.remove(j);
                    log.log(Level.INFO, "Job with ID " + id + " has been computed succefully.");
                    storeJobAction(j, jr.getOwner().getTargetId(), JobConstants.JOB_RESULT);
                    wakeUp();
                    return GenericResponses.OK;
                }
            }
        }
        log.log(Level.WARNING, "JobResult received for illegal UUID - " + id);
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
            l = new LinkedList<JobAction>();
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
                    JobTask jt = new JobTask(job.getId(), JobConstants.JOB_CANCEL, null);
                    jr.getOwner().sendData(jt);
                    job.setStatus(JobStatus.CANCELED);
                    jr.updateTime();
                    it.remove();
                    jobHistory.remove(j);
                    log.log(Level.INFO, "Job with ID " + j.getId() + " has been cancelled by server.");
                }
            }
        }
    }
}
