package cz.tul.comm.job;

import cz.tul.comm.communicator.Communicator;

/**
 * Interface allowing clients request job from server.
 *
 * @author Petr Ječmen
 */
public interface JobRequestManager {

    /**
     * Request an extra job from server.
     * @param comm target client
     */
    void requestJob(final Communicator comm);
}
