package cz.tul.javaccl;

import java.util.UUID;

/**
 * GlobalConstants used thorought app.
 *
 * @author Petr Ječmen
 */
public abstract class GlobalConstants {

    /**
     * default server port
     */
    public static final int DEFAULT_PORT = 5252;
    /**
     * default timeout in ms
     */
    private static int DEFAULT_TIMEOUT = 30000;
    /**
     * data used as question for client discovery
     */
    public static final String DISCOVERY_QUESTION = "DISCOVER_CLIENT";
    /**
     * data used as an identifier for distributing server info amongs clients
     */
    public static final String DISCOVERY_INFO = "DISCOVER_SERVER";
    /**
     * Server UUID
     */
    public static final UUID ID_SERVER = UUID.fromString("a1b248d1-949b-01e4-a84b-000000000001");
    /**
     * UUID of job manager
     */
    public static final UUID ID_JOB_MANAGER = UUID.fromString("a1b248d1-949b-01e4-a84b-000000000002");
    /**
     * UUID for system message handler
     */
    public static final UUID ID_SYS_MSG = UUID.fromString("a1b248d1-949b-01e4-a84b-000000000003");
    /**
     * Splitting character
     */
    public static final String DELIMITER = "-";
    /**
     * IP for loopback communication
     */
    public static final String IP_LOOPBACK = "127.0.0.1";    

    public static int getDEFAULT_TIMEOUT() {
        return DEFAULT_TIMEOUT;
    }    

    public static void setDEFAULT_TIMEOUT(int DEFAULT_TIMEOUT) {
        GlobalConstants.DEFAULT_TIMEOUT = DEFAULT_TIMEOUT;
    }
    
}
