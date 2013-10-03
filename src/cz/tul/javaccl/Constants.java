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
    int DEFAULT_PORT = 5252;
    /**
     * default timeout in ms
     */
    int DEFAULT_TIMEOUT = 30000;
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
    UUID ID_SERVER = UUID.fromString("a1b248d1-949b-01e4-a84b-000000000001");
    /**
     * UUID of job manager
     */
    UUID ID_JOB_MANAGER = UUID.fromString("a1b248d1-949b-01e4-a84b-000000000002");
    /**
     * UUID for system message handler
     */
    UUID ID_SYS_MSG = UUID.fromString("a1b248d1-949b-01e4-a84b-000000000003");
    /**
     * Splitting character
     */
    String DELIMITER = "-";
    /**
     * IP for loopback communication
     */
    String IP_LOOPBACK = "127.0.0.1";
}
