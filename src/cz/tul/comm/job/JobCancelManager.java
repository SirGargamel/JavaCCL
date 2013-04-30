package cz.tul.comm.job;

/**
 *
 * @author Petr Ječmen
 */
public interface JobCancelManager {

    void cancelJob(final ServerSideJob job);
    
}
