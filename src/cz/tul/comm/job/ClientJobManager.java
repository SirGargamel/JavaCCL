package cz.tul.comm.job;

import java.util.UUID;

/**
 *
 * @author Petr Ječmen
 */
public interface ClientJobManager {

    void acceptJob(final UUID jobId);

    void cancelJob(final UUID jobId);

    Object requestData(final UUID jobId, final Object dataId);

    void submitResult(final UUID jobId, final Object result);
    
}
