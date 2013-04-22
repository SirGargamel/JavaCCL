package cz.tul.comm.job;

import cz.tul.comm.communicator.Communicator;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

/**
 *
 * @author Petr Ječmen
 */
public class JobAction {

    private final UUID jobId;
    private final Calendar actionTime;
    private final Communicator owner;
    private final String msgHeader;

    public JobAction(UUID jobId, Communicator owner, String msgHeader) {
        this.jobId = jobId;
        this.owner = owner;
        this.msgHeader = msgHeader;
        this.actionTime = Calendar.getInstance(Locale.getDefault());
    }

    public UUID getJobId() {
        return jobId;
    }

    public Calendar getActionTime() {
        return actionTime;
    }

    public Communicator getOwner() {
        return owner;
    }

    public String getMsgHeader() {
        return msgHeader;
    }
}
