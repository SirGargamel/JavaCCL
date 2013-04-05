package cz.tul.comm.messaging.job;

import java.util.UUID;

/**
 *
 * @author Petr Ječmen
 */
public interface Job {

    Object getTask();

    Object getResult(final boolean blockingGet);

    void cancelJob();

    JobStatus getStatus();

    boolean isDone();

    boolean isCanceled();
    
    UUID getId();
}
