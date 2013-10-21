package cz.tul.javaccl.job.client;

import cz.tul.javaccl.exceptions.ConnectionException;
import java.util.UUID;

/**
 * Interface job managing job computation on the client side.
 *
 * @author Petr Ječmen
 */
public interface ClientJobManager {

    /**
     * Accept job
     *
     * @param jobId UUID of the job
     * @throws ConnectionException server could not be contacted
     */
    void acceptJob(final UUID jobId) throws ConnectionException;

    /**
     * Cancel the computation of the job on this client.
     *
     * @param jobId UUID of the job
     * @throws ConnectionException server could not be contacted
     */
    void cancelJob(final UUID jobId) throws ConnectionException;

    /**
     * Request data from server
     *
     * @param jobId UUID of the job
     * @param dataId ID of the requested data
     * @return requsted data
     * @throws ConnectionException server could not be contacted
     */
    Object requestData(final UUID jobId, final Object dataId) throws ConnectionException;

    /**
     * Send the result of computation to the server.
     *
     * @param jobId UUID of the job
     * @param result result of the computation
     * @throws ConnectionException server could not be contacted
     */
    void submitResult(final UUID jobId, final Object result) throws ConnectionException;
}
