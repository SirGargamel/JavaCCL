package cz.tul.javaccl.job.server;

import cz.tul.javaccl.exceptions.ConnectionException;

/**
 *
 * @author Petr Ječmen
 */
public interface JobCancelManager {

    /**
     * Cancel the job and never compute it again
     *
     * @param job job to cancel
     * @throws ConnectionException could not contact the client that is
     * computing the job
     */
    void cancelJobByServer(final ServerSideJob job) throws ConnectionException;
    
}
