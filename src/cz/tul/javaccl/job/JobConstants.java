package cz.tul.javaccl.job;

/**
 * Message headers used in communication.
 *
 * @author Petr Ječmen
 */
public interface JobConstants {

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
    /**
     * Sending job for computation.
     */
    String JOB_TASK = "jobTask";
    /**
     * Telling the server how many concurrent job a client can compute.
     */
    String JOB_COUNT = "jobCount";

    /**
     * Telling the server how complex the job can be
     */
    String JOB_COMPLEXITY = "jobComplexity";
    /**
     * data contains new settings for client
     */
    String JOB_CLIENT_SETTINGS = "jobClientSettings";

    /**
     * default job complexity
     */
    int DEFAULT_COMPLEXITY = 1000;
}
