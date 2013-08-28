package cz.tul.comm.job.client;

import cz.tul.comm.exceptions.ConnectionException;
import java.util.UUID;

/**
 * Interface offering methods to client to handle assignment.
 *
 * @author Petr Ječmen
 */
public interface Assignment {

    /**
     * @return jobs task
     * @throws ConnectionException could not contact the server to tell him that
     * the client accepts the assignment
     */
    Object getTask() throws ConnectionException;

    /**
     * Request data from server.
     *
     * @param dataId data identificator
     * @return requested data
     * @throws ConnectionException could not contact the server to complete the
     * request
     */
    Object requestData(final Object dataId) throws ConnectionException;

    /**
     * Submit result for this assignment and send it to server.
     *
     * @param result computation result
     * @throws ConnectionException could not contact the server to send the
     * result
     */
    void submitResult(final Object result) throws ConnectionException;

    /**
     * Cancel job computation.
     *
     * @param reason descritpion why the job was cancelled
     * @throws ConnectionException could not contact the server to cancel the
     * assignment
     */
    void cancel(final String reason) throws ConnectionException;

    /**
     * @return assignments ID
     */
    UUID getId();

    /**
     * @return true if computation has been completed.
     */
    boolean isDone();
}
