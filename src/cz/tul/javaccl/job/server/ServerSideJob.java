package cz.tul.javaccl.job.server;

import cz.tul.javaccl.exceptions.ConnectionException;
import cz.tul.javaccl.job.JobConstants;
import cz.tul.javaccl.job.JobStatus;
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
    private final int complexity;
    private Object result;
    private JobStatus jobStatus;

    ServerSideJob(final Object task, final JobCancelManager jobCancelManager, final int complexity) throws IllegalArgumentException {
        jcm = jobCancelManager;
        this.task = task;
        jobStatus = JobStatus.SUBMITTED;
        jobId = UUID.randomUUID();
        this.complexity = complexity;
    }

    ServerSideJob(final Object task, final JobCancelManager jobCancelManager) throws IllegalArgumentException {
        this(task, jobCancelManager, JobConstants.DEFAULT_COMPLEXITY);
    }

    @Override
    public Object getTask() {
        return task;
    }

    @Override
    public Object getResult(final boolean blockingGet) {
        if (blockingGet) {
            while (!isDone()) {
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

    /**
     * @param result result of the job
     */
    public synchronized void setResult(Object result) {
        this.result = result;
        setStatus(JobStatus.FINISHED);
        this.notifyAll();
    }

    @Override
    public synchronized void cancelJob() throws ConnectionException {
        jcm.cancelJobByServer(this);
        this.notifyAll();
    }

    /**
     * @param jobStatus new job status
     */
    public synchronized void setStatus(final JobStatus jobStatus) {
        this.jobStatus = jobStatus;
        this.notifyAll();
    }

    @Override
    public synchronized JobStatus getStatus() {
        return jobStatus;
    }

    @Override
    public synchronized boolean isDone() {
        return jobStatus.equals(JobStatus.FINISHED) || isCanceled();
    }

    @Override
    public synchronized boolean isCanceled() {
        return jobStatus.equals(JobStatus.ERROR) || jobStatus.equals(JobStatus.CANCELED);
    }

    @Override
    public UUID getId() {
        return jobId;
    }

    /**
     * @return this jobs complexity
     */
    public int getComplexity() {
        return complexity;
    }
}
