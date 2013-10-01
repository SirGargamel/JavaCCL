package cz.tul.javaccl.job.server;

import cz.tul.javaccl.communicator.Communicator;
import java.util.Calendar;

/**
 *
 * @author Petr Jecmen
 */
public class JobRecord {

    private final ServerSideJob job;
    private Communicator owner;
    private Calendar lastAction;

    public JobRecord(ServerSideJob job) {
        this.job = job;
        owner = null;
        updateTime();
    }

    public final ServerSideJob getJob() {
        return job;
    }

    public final Communicator getOwner() {
        return owner;
    }

    public final Calendar getLastAction() {
        return lastAction;
    }

    public final void setOwner(Communicator owner) {
        this.owner = owner;
        updateTime();
    }
    
    public final void updateTime() {
        lastAction = Calendar.getInstance();
    }
}
