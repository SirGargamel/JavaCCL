package cz.tul.comm.messaging.job;

import java.io.Serializable;
import java.util.UUID;

/**
 * Data class for sending assignments to client.
 *
 * @author Petr Ječmen
 */
public class JobTask implements Serializable {

    private final UUID jobId;
    private final Object task;

    /**
     * Init new JobTasj
     * @param jobId jobs ID
     * @param task job task
     */
    public JobTask(UUID jobId, Object task) {
        this.jobId = jobId;
        this.task = task;
    }

    /**     
     * @return jobs ID
     */
    public UUID getJobId() {
        return jobId;
    }

    /**     
     * @return job task
     */
    public Object getTask() {
        return task;
    }
}
