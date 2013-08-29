package cz.tul.comm.job.client;

import cz.tul.comm.exceptions.ConnectionException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client side of job compuatation. Handles client operations.
 *
 * @author Petr Ječmen
 */
public class ClientSideJob implements Assignment {

    private static final Logger log = Logger.getLogger(ClientSideJob.class.getName());
    private final Object task;
    private final ClientJobManager jobManager;
    private final UUID jobId;    
    private boolean isDone;

    /**
     * Create new instance.
     *
     * @param task task for computation
     * @param jobId jobs ID
     * @param jobManager job management interface
     */
    public ClientSideJob(final Object task, final UUID jobId, final ClientJobManager jobManager) {
        this.task = task;
        this.jobId = jobId;
        this.jobManager = jobManager;

        isDone = false;        
    }

    @Override
    public Object requestData(Object dataId) throws ConnectionException {
        log.log(Level.CONFIG, "Requesting data with ID {0} for task with ID {1}", new Object[]{dataId, jobId});
        return jobManager.requestData(jobId, dataId);
    }

    @Override
    public void submitResult(Object result) throws ConnectionException {
        log.log(Level.CONFIG, "Submitting result for task with ID {0}", jobId);
        isDone = true;        
        jobManager.submitResult(jobId, result);
    }

    @Override
    public Object getTask() throws ConnectionException {
        log.log(Level.CONFIG, "Accepting task with ID {0}", jobId);
        jobManager.acceptJob(jobId);
        return task;
    }

    @Override
    public void cancel(final String reason) throws ConnectionException {
        log.log(Level.CONFIG, "Canceling task with ID {0}", jobId);        
        jobManager.cancelJob(jobId);
    }

    @Override
    public UUID getId() {
        return jobId;
    }

    @Override
    public boolean isDone() {
        return isDone;
    }    
}
