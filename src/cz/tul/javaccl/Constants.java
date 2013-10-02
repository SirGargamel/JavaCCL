package cz.tul.javaccl;

import java.util.UUID;

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
     * default timeout in ms
     */
    int DEFAULT_TIMEOUT = 30_000;
    /**
     * data used as question for client discovery
     */
    String DISCOVERY_QUESTION = "DISCOVER_CLIENT";
    /**
     * data used as an identifier for distributing server info amongs clients
     */
    String DISCOVERY_INFO = "DISCOVER_SERVER";
    /**
     * Server UUID
     */
    UUID ID_SERVER = UUID.randomUUID();
    /**
     * UUID of job manager
     */
    UUID ID_JOB_MANAGER = UUID.randomUUID();
    /**
     * UUID for system message handler
     */
    UUID ID_SYS_MSG = UUID.randomUUID();
    /**
     * Spliitting character
     */
    String DELIMITER = "-";
}
