package cz.tul.javaccl.job.server;

import cz.tul.javaccl.communicator.Communicator;
import java.util.Calendar;

/**
 * Record holding info about job execution.
 *
 * @author Petr Jecmen
 */
public class JobRecord {

    private final ServerSideJob job;
    private Communicator owner;
    private Calendar lastAction;

    /**
     * new instance of record
     *
     * @param job job for recording
     */
    public JobRecord(ServerSideJob job) {
        this.job = job;
        owner = null;
        updateTime();
    }

    /**
     * @return instance of recorded job
     */
    public final ServerSideJob getJob() {
        return job;
    }

    /**
     * @return communicator for jobs owner
     */
    public final Communicator getOwner() {
        return owner;
    }

    /**
     * @return date and time of last action
     */
    public final Calendar getLastAction() {
        return lastAction;
    }

    /**
     * @param owner new owner
     */
    public final void setOwner(Communicator owner) {
        this.owner = owner;
        updateTime();
    }

    /**
     * Mark actual time as time of last action.
     */
    public final void updateTime() {
        lastAction = Calendar.getInstance();
    }
}
