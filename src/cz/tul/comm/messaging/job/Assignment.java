package cz.tul.comm.messaging.job;

import java.util.UUID;

/**
 * Interface offering methods to client to handle assignment.
 *
 * @author Petr Ječmen
 */
public interface Assignment {

    /**
     * @return jobs task
     */
    Object getTask();

    /**
     * Request data from server.
     *
     * @param dataId data identificator
     * @return requested data
     */
    Object requestData(final Object dataId);

    /**
     * Submit result for this assignment and send it to server.
     *
     * @param result computation result
     * @return true for successfull sending 
     */
    boolean submitResult(final Object result);

    /**
     * Cancel job computation.
     *
     * @param reason descritpion why the job was cancelled
     */
    void cancel(final String reason);

    /**
     * @return assignments ID
     */
    UUID getId();
}
