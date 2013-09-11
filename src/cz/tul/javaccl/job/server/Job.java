package cz.tul.javaccl.job.server;

import cz.tul.javaccl.exceptions.ConnectionException;
import cz.tul.javaccl.job.JobStatus;
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
     * Cancel job computaiton. This job wont be computed (wont be given to
     * another client for computation, handover to another client must be done
     * from clients side).
     *
     * @throws ConnectionException could not contact the client that is
     * computing the job
     */
    void cancelJob() throws ConnectionException;

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
