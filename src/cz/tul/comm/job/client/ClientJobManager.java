package cz.tul.comm.job.client;

import cz.tul.comm.exceptions.ConnectionException;
import java.util.UUID;

/**
 *
 * @author Petr Ječmen
 */
public interface ClientJobManager {

    void acceptJob(final UUID jobId) throws ConnectionException;

    void cancelJob(final UUID jobId) throws ConnectionException;

    Object requestData(final UUID jobId, final Object dataId) throws ConnectionException;

    void submitResult(final UUID jobId, final Object result) throws ConnectionException;
    
}
