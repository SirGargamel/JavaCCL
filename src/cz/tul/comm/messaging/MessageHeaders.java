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
    String MSG_PULL_REQUEST = "msgPullRequest";
    String CLIENT_IP_PORT_QUESTION = "clientIpPortQuestion";
}
