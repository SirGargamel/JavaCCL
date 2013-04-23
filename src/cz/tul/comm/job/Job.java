package cz.tul.comm.job;

import java.util.UUID;

/**
 * Interface offering action to server for job handling.
 *
 * @author Petr Ječmen
 */
public interface Job {

    /**
     * @return jobs task
     */
    Object getTask();

    /**
     * Return result of computation (or null if the computation is not completed
     * now)
     *
     * @param blockingGet true if the method should block until result is
     * received
     * @return computation result
     */
    Object getResult(final boolean blockingGet);

    /**
     * Cancel job computaiton.
     */
    void cancelJob();

    /**
     * @return current job status
     */
    JobStatus getStatus();

    /**
     * @return true if job has comleted (result has been received) or the job
     * has been cancelled
     */
    boolean isDone();

    /**
     * @return true if the job has been cancelled by client or server, or some
     * other error has occured
     */
    boolean isCanceled();

    /**
     * @return jobs ID
     */
    UUID getId();
}
