package cz.tul.comm.job;

import cz.tul.comm.communicator.Communicator;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

/**
 * Class for storing actions that happened during job computation.
 *
 * @author Petr Ječmen
 */
public class JobAction {

    private final UUID jobId;
    private final Calendar actionTime;
    private final Communicator owner;
    private final String actionDescription;

    /**     
     * @param jobId jobs ID
     * @param owner job owner
     * @param msgHeader action description
     */
    public JobAction(UUID jobId, Communicator owner, String msgHeader) {
        this.jobId = jobId;
        this.owner = owner;
        this.actionDescription = msgHeader;
        this.actionTime = Calendar.getInstance(Locale.getDefault());
    }

    /**
     * @return jobs ID
     */
    public UUID getJobId() {
        return jobId;
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
    public Communicator getOwner() {
        return owner;
    }

    /**
     * @return action description
     */
    public String getActionDescription() {
        return actionDescription;
    }
}
