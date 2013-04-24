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
    int DEFAULT_PORT = 5_252;     
    /**
     * data used as question for client discovery
     */
    String DISCOVERY_QUESTION = "DISCOVER_CLIENT";    
    /**
     * character splitting port number from rest of the response
     */
    String DISCOVERY_QUESTION_DELIMITER = ":";
}
