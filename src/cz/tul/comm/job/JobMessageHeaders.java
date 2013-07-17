package cz.tul.comm.job;

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
    String JOB_TASK = "jobTask";
    String JOB_COUNT = "jobCount";
}
