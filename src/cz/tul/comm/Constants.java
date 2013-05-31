package cz.tul.comm;

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
     * data used as question for client discovery
     */
    String DISCOVERY_QUESTION = "DISCOVER_CLIENT";
    /**
     * character splitting port number from rest of the response
     */
    String DELIMITER = ":";
    UUID ID_SERVER = UUID.fromString("00000000-0000-0000-0000-000000000000");
    UUID ID_JOB_MANAGER = UUID.fromString("00001111-2222-3333-4444-555566667777");
    UUID ID_SYS_MSG = UUID.fromString("77776666-5555-4444-3333-222211110000");
}
