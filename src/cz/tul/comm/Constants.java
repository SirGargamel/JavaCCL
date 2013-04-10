package cz.tul.comm;

/**
 * Constants used thorought app.
 *
 * @author Petr Ječmen
 */
public interface Constants {

    /**
     * default server port
     */
    int DEFAULT_PORT = 5252;     
    /**
     * data used as question for client discovery
     */
    String DISCOVERY_QUESTION = "DISCOVER_CLIENT";
    /**
     * response for client discovery if client has been found.
     */
    String DISCOVERY_RESPONSE = "DISCOVER_REGISTRATION";    
    /**
     * character splitting port number from rest of the response
     */
    String DISCOVERY_RESPONSE_DELIMITER = ":";
}
