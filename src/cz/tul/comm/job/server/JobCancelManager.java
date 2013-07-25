package cz.tul.comm.job.server;

import cz.tul.comm.exceptions.ConnectionException;

/**
 *
 * @author Petr Ječmen
 */
public interface JobCancelManager {

    void cancelJobByServer(final ServerSideJob job) throws ConnectionException;
    
}
