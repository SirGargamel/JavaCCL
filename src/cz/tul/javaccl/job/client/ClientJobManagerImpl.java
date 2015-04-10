package cz.tul.javaccl.job.client;

import cz.tul.javaccl.GenericResponses;
import cz.tul.javaccl.client.ServerInterface;
import cz.tul.javaccl.exceptions.ConnectionException;
import cz.tul.javaccl.job.JobConstants;
import cz.tul.javaccl.job.JobTask;
import cz.tul.javaccl.messaging.Identifiable;
import cz.tul.javaccl.socket.Listener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of {@link ClientJobManager} interface.
 *
 * @author Petr Ječmen
 */
public class ClientJobManagerImpl implements Listener<Identifiable>, ClientJobManager {

    private static final Logger LOG = Logger.getLogger(ClientJobManagerImpl.class.getName());
    private static final int WAIT_TIME = 500;
    private AssignmentListener assignmentListener;
    private final ServerInterface server;
    private final Map<UUID, ClientSideJob> jobs;    
    private final Set<ClientSideJob> runningJobs;
    private ExecutorService exec;
    private int maxJobCount;

    /**
     * New instance.
     *
     * @param server interface for sending data to server
     */
    public ClientJobManagerImpl(ServerInterface server) {
        this.server = server;
        jobs = new HashMap<UUID, ClientSideJob>();        
        assignmentListener = null;
        runningJobs = new HashSet<ClientSideJob>();

        maxJobCount = 1;
        exec = Executors.newFixedThreadPool(maxJobCount);
    }

    /**
     * Set listener which will receive all incoming assignments.
     *
     * @param assignmentListener new assignment listener
     */
    public void setAssignmentListener(AssignmentListener assignmentListener) {
        this.assignmentListener = assignmentListener;
    }

    @Override
    public void submitResult(final UUID jobId, final Object result) throws ConnectionException {
        sendDataToServer(jobId, JobConstants.JOB_RESULT, result);
        runningJobs.remove(jobs.get(jobId));
    }

    @Override
    public void cancelJob(final UUID jobId) throws ConnectionException {
        sendDataToServer(jobId, JobConstants.JOB_CANCEL, null);
        runningJobs.remove(jobs.get(jobId));
    }

    @Override
    public void acceptJob(final UUID jobId) throws ConnectionException {
        sendDataToServer(jobId, JobConstants.JOB_ACCEPT, null);
        runningJobs.add(jobs.get(jobId));
    }

    @Override
    public Object requestData(final UUID jobId, final Object dataId) throws ConnectionException {
        return sendDataToServer(jobId, JobConstants.JOB_DATA_REQUEST, dataId);
    }

    private Object sendDataToServer(final UUID jobId, final String header, final Object result) throws ConnectionException {
        waitForSever();
        final JobTask jt = new JobTask(jobId, header, result);
        return server.getServerComm().sendData(jt);
    }

    private void waitForSever() {
        if (!server.isServerUp()) {
            LOG.fine("Waiting for server to become online.");
            do {
                synchronized (this) {
                    try {
                        this.wait(WAIT_TIME);
                    } catch (InterruptedException ex) {
                        LOG.log(Level.WARNING, "Waiting for server being available for data request has been interrupted.");
                        LOG.log(Level.FINE, "Waiting for server being available for data request has been interrupted.", ex);
                    }
                }
            } while (!server.isServerUp());
        }
    }

    @Override
    public Object receiveData(final Identifiable data) {
        Object result = GenericResponses.ILLEGAL_DATA;
        if (data instanceof JobTask) {
            result = GenericResponses.ILLEGAL_HEADER;
            final JobTask jt = (JobTask) data;
            final UUID id = jt.getJobId();
            final String descr = jt.getTaskDescription();
            if (descr != null) {
                if (descr.equals(JobConstants.JOB_TASK)) {
                    if (assignmentListener != null) {
                        if (runningJobs.size() < maxJobCount) {
                            final ClientSideJob job = new ClientSideJob(jt.getTask(), id, this);
                            jobs.put(job.getId(), job);
                            exec.execute(new Runnable() {
                                @Override
                                public void run() {
                                    assignmentListener.receiveTask(job);
                                }
                            });
                            result = GenericResponses.OK;                            
                        } else {
                            result = GenericResponses.CANCEL;                            
                        }
                    } else {
                        result = GenericResponses.GENERAL_ERROR;
                    }
                } else if (descr.equals(JobConstants.JOB_CANCEL)) {
                    jobs.remove(id);
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            assignmentListener.cancelTask(jobs.get(id));
                        }
                    });
                    result = GenericResponses.OK;
                }
            }
        }
        return result;
    }

    @Override
    public void setMaxNumberOfConcurrentAssignments(int assignmentCount) {
        maxJobCount = assignmentCount;
        exec = Executors.newFixedThreadPool(maxJobCount);
    }
}
