package cz.tul.comm.messaging.job;

import java.util.UUID;

/**
 *
 * @author Petr Ječmen
 */
public interface Assignment {

    Object getTask();

    Object requestData(final Object dataId);

    boolean submitResult(final Object result);
    
    void cancelJob(final String reason);
    
    UUID getId();
}
