package cz.tul.comm.job.server;

import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

/**
 * Class for storing actions that happened during job computation.
 *
 * @author Petr Ječmen
 */
public class JobAction {

    private final Job job;
    private final Calendar actionTime;
    private final UUID ownerId;
    private final String actionDescription;

    /**     
     * @param jobId jobs ID
     * @param owner job owner
     * @param msgHeader action description
     */
    public JobAction(final Job job, final UUID ownerId, final String msgHeader) {
        this.job = job;
        this.ownerId = ownerId;
        this.actionDescription = msgHeader;
        this.actionTime = Calendar.getInstance(Locale.getDefault());
    }

    /**
     * @return jobs ID
     */
    public UUID getJobId() {
        return job.getId();
    }

    /**
     * @return time at which the action has happened
     */
    public Calendar getActionTime() {
        return actionTime;
    }

    /**
     * @return jobs owner
     */
    public UUID getOwnerId() {
        return ownerId;
    }

    /**
     * @return action description
     */
    public String getActionDescription() {
        return actionDescription;
    }
}
