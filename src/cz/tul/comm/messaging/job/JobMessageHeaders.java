package cz.tul.comm.messaging.job;

/**
 *
 * @author Petr Ječmen
 */
public interface JobMessageHeaders {

    String JOB_ACCEPT = "jobAccept";
    String JOB_CANCEL = "jobCancel";
    String JOB_DATA_REQUEST = "jobDataRequest";
    String JOB_RESULT = "jobResult";
}
