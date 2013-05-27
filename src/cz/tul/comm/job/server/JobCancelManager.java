package cz.tul.comm.job.server;

/**
 *
 * @author Petr Ječmen
 */
public interface JobCancelManager {

    void cancelJob(final ServerSideJob job);
    
}
