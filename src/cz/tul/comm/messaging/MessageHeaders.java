package cz.tul.comm.messaging;

/**
 * Predefined headers for messages.
 *
 * @author Gargamel
 */
public interface MessageHeaders {
    
    String LOGIN = "login";
    String STATUS = "status";
    // keep alives should be ignored as they function only for cummunication check
    String KEEP_ALIVE = "keepAlive";
}
