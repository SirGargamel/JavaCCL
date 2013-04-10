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
     * question for client status
     */
    String STATUS = "status";
    /**
     * keep alives should be ignored as they function only for cummunication
     * check
     */
    String KEEP_ALIVE = "keepAlive";
    /**
     * job assignment
     */
    String JOB = "job";
}
