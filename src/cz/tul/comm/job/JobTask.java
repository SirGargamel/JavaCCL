package cz.tul.comm.job;

import cz.tul.comm.Constants;
import cz.tul.comm.messaging.Identifiable;
import java.io.Serializable;
import java.util.UUID;

/**
 * Data class for sending assignments to client.
 *
 * @author Petr Ječmen
 */
public class JobTask implements Serializable, Identifiable {

    private final UUID jobId;
    private final String taskDescription;
    private final Object task;

    /**
     * Init new JobTasj
     *
     * @param jobId jobs ID
     * @param task job task
     */
    public JobTask(final UUID jobId, final String taskDescritpion, final Object task) {
        this.jobId = jobId;
        this.taskDescription = taskDescritpion;
        this.task = task;
    }

    /**
     * @return jobs ID
     */
    public UUID getJobId() {
        return jobId;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    /**
     * @return job task
     */
    public Object getTask() {
        return task;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("JobTask - ");
        sb.append(jobId);        
        sb.append(" - ");
        sb.append(taskDescription);
        if (task != null) {
            sb.append(" - ");
            sb.append("[");
            sb.append(task.toString());
            sb.append("]");
        }

        return sb.toString();
    }

    @Override
    public Object getId() {
        return Constants.ID_JOB_MANAGER;
    }
}
