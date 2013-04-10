package cz.tul.comm.messaging.job;

/**
 * Message headers used in communication.
 *
 * @author Petr Ječmen
 */
public interface JobMessageHeaders {

    /**
     * Confirm job task by client.
     */
    String JOB_ACCEPT = "jobAccept";
    /**
     * Cancel job computation.
     */
    String JOB_CANCEL = "jobCancel";
    /**
     * Request data from server.
     */
    String JOB_DATA_REQUEST = "jobDataRequest";
    /**
     * Submit job result.
     */
    String JOB_RESULT = "jobResult";
}
