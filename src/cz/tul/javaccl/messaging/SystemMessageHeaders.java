package cz.tul.javaccl.messaging;

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
}
