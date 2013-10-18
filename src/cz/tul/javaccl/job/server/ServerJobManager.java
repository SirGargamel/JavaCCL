package cz.tul.javaccl.job.server;

import java.util.Collection;

/**
 * Interface for job management. Offers baisc function needed for successfull
 * computation.
 *
 * @author Petr Ječmen
 */
public interface ServerJobManager {
    

    /**
     * Force all jobs to stop computation.
     */
    void stopAllJobs();

    /**
     * Submit job for computation
     *
     * @param task jobs task
     * @return interface for job control
     * @throws IllegalArgumentException Task object cannot be serialized 
     */
    Job submitJob(final Object task) throws IllegalArgumentException;

    /**
     * Method waits until all jobs are completed.
     */
    void waitForAllJobs();
}
