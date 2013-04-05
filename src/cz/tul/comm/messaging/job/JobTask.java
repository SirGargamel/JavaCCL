package cz.tul.comm.messaging.job;

import java.io.Serializable;
import java.util.UUID;

/**
 *
 * @author Petr Ječmen
 */
public class JobTask implements Serializable {

    private final UUID jobId;
    private final Object task;

    public JobTask(UUID jobId, Object task) {
        this.jobId = jobId;
        this.task = task;
    }

    public UUID getJobId() {
        return jobId;
    }

    public Object getTask() {
        return task;
    }
    
}
