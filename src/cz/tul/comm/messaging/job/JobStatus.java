package cz.tul.comm.messaging.job;

/**
 * Possible states of job computation.
 *
 * @author Petr Ječmen
 */
public enum JobStatus {

    /**
     * jobs has been stored to job queue and is waiting for aviable client
     */
    SUBMITTED,
    /**
     * job has been sent co client
     */
    SENT,
    /**
     * client has confirmed job task
     */
    ASSIGNED,
    /**
     * client has finished computing and sent result back to server
     */
    COMPUTED,
    /**
     * job has been cancelled
     */
    CANCELED,
    /**
     * job computation failed
     */
    ERROR;
}
