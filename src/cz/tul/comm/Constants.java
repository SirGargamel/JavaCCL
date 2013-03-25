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
    int DEFAULT_PORT_S = 5252;
    /**
     * default client port
     */
    int DEFAULT_PORT_C = 5253;
    /**
     * default port for client discovery
     */
    int DEFAULT_PORT_DISCOVERY = 5254;
    /**
     * data used as question for client discovery
     */
    String DISCOVERY_QUESTION = "DISCOVER_CLIENT";
    /**
     * response for client discovery if client has been found.
     */
    String DISCOVERY_RESPONSE = "DISCOVER_REGISTRATION";
}
