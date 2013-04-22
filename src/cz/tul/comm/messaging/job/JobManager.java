package cz.tul.comm.messaging.job;

import java.util.Collection;

/**
 *
 * @author Petr Ječmen
 */
public interface JobManager {

    Collection<Job> getAllJobs();

    void stopAllJobs();

    /**
     * Submit job for computation
     *
     * @param task jobs task
     * @return interface for job control
     */
    Job submitJob(final Object task);

    void waitForAllJobs();
    
}
