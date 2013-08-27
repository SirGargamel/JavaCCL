package cz.tul.comm.messaging;

/**
 * Predefined headers for messages.
 *
 * @author Gargamel
 */
public interface SystemMessageHeaders {

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
    String STATUS_CHECK = "statusCheck";
    String MSG_PULL_REQUEST = "msgPullRequest";    
}