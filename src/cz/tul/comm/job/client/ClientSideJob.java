package cz.tul.comm.job.client;

import cz.tul.comm.exceptions.ConnectionException;
import cz.tul.comm.job.JobStatus;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Client side of job compuatation. Handles client operations.
 * @author Petr Ječmen
 */
public class ClientSideJob implements Assignment {

    private static final Logger log = Logger.getLogger(ClientSideJob.class.getName());
    private final Object task;
    private final ClientJobManager jobManager;    
    private final UUID jobId;
    private JobStatus jobStatus;
    private boolean isDone;

    /**
     * Create new instance.
     *
     * @param task task for computation
     * @param jobId jobs ID
     * @param comm server communicator
     * @param listenerRegistrator listener registrator
     * @param taskListener task listener for job cancelation
     */
    public ClientSideJob(final Object task, final UUID jobId, final ClientJobManager jobManager) {
        this.task = task;        
        this.jobId = jobId; 
        this.jobManager = jobManager;
        
        isDone = false;
        jobStatus = JobStatus.ACCEPTED;
    }    

    @Override
    public Object requestData(Object dataId) throws ConnectionException {
        return jobManager.requestData(jobId, dataId);
    }

    @Override
    public void submitResult(Object result) throws ConnectionException {        
        isDone = true;
        jobStatus = JobStatus.FINISHED;
        jobManager.submitResult(jobId, result);
    }

    @Override
    public Object getTask() throws ConnectionException {
        jobManager.acceptJob(jobId);
        return task;
    }

    @Override
    public void cancel(final String reason) throws ConnectionException {
        jobStatus = JobStatus.CANCELED;
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

    public JobStatus getStatus() {
        return jobStatus;
    }
}
