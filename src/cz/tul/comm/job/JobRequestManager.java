package cz.tul.comm.job;

import cz.tul.comm.communicator.Communicator;

/**
 *
 * @author Petr Ječmen
 */
public interface JobRequestManager {

    void requestJob(final Communicator comm);
    
}