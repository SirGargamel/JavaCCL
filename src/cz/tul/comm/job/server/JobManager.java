package cz.tul.comm.job.server;

import java.util.Collection;

/**
 * Interface for job management. Offers baisc function needed for successfull
 * computation.
 *
 * @author Petr Ječmen
 */
public interface JobManager {

    /**
     * @return collection of all submitted jobs
     */
    Collection<Job> getAllJobs();

    /**
     * Force all jobs to stop computation.
     */
    void stopAllJobs();

    /**
     * Submit job for computation
     *
     * @param task jobs task
     * @return interface for job control
     */
    Job submitJob(final Object task) throws IllegalArgumentException;

    /**
     * Method waits until all jobs are completed.
     */
    void waitForAllJobs();
}
