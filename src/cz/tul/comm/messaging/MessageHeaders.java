package cz.tul.comm.messaging;

/**
 * Predefined headers for messages.
 *
 * @author Gargamel
 */
public interface MessageHeaders {

    /**
     * client registration
     */
    String LOGIN = "login";
    /**
     * client is logging out
     */
    String LOGOUT = "logout";    
    /**
     * keep alives should be ignored as they function only for cummunication
     * check
     */
    String KEEP_ALIVE = "keepAlive";
    /**
     * job assignment
     */
    String JOB = "job";
    /**
     * request extra job from server.
     */
    String JOB_REQUEST = "jobRequest";
}
