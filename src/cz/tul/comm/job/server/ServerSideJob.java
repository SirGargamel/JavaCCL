package cz.tul.comm.job.server;

import cz.tul.comm.Utils;
import cz.tul.comm.exceptions.ConnectionException;
import cz.tul.comm.job.JobStatus;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side of job computation.
 *
 * @author Petr Ječmen
 */
public class ServerSideJob implements Job {

    private static final Logger log = Logger.getLogger(ServerSideJob.class.getName());
    private final JobCancelManager jcm;
    private final Object task;
    private final UUID jobId;
    private Object result;
    private JobStatus jobStatus;

    ServerSideJob(final Object task, final JobCancelManager jobCancelManager) throws IllegalArgumentException {
        jcm = jobCancelManager;
        if (Utils.checkSerialization(task)) {
            this.task = task;
        } else {
            throw new IllegalArgumentException("JobTasks data (and all of its members) must be serializable (eg. implement Serializable or Externalizable interface.)");
        }
        jobStatus = JobStatus.SUBMITTED;
        jobId = UUID.randomUUID();
    }

    @Override
    public Object getTask() {
        return task;
    }

    @Override
    public Object getResult(final boolean blockingGet) {
        if (blockingGet) {
            while (result == null) {
                synchronized (this) {
                    try {
                        this.wait();
                    } catch (InterruptedException ex) {
                        log.log(Level.WARNING, "Job waiting for result has been interrupted", ex);
                    }
                }
            }
        }

        return result;
    }

    public void setResult(Object result) {
        this.result = result;
        synchronized (this) {
            this.notify();
        }
    }

    @Override
    public void cancelJob() throws ConnectionException {
        log.log(Level.CONFIG, "Canceling job with ID {0}.", jobId);
        jcm.cancelJob(this);
        jobStatus = JobStatus.CANCELED;
        
    }

    /**
     * @param jobStatus new job status
     */
    public void setStatus(final JobStatus jobStatus) {
        this.jobStatus = jobStatus;
    }

    @Override
    public JobStatus getStatus() {
        return jobStatus;
    }

    @Override
    public boolean isDone() {
        return jobStatus.equals(JobStatus.FINISHED) || isCanceled();
    }

    @Override
    public boolean isCanceled() {
        return jobStatus.equals(JobStatus.ERROR) || jobStatus.equals(JobStatus.CANCELED);
    }

    @Override
    public UUID getId() {
        return jobId;
    }
}
