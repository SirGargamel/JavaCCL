package cz.tul.javaccl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * GlobalConstants used thorought app.
 *
 * @author Petr Ječmen
 */
public final class GlobalConstants {

    private static final Logger LOG = Logger.getLogger(GlobalConstants.class.getName());
    /**
     * default server port
     */
    public static final int DEFAULT_PORT = 5252;
    /**
     * UUID of job manager
     */
    public static final UUID ID_JOB_MANAGER = UUID.fromString("a1b248d1-949b-01e4-a84b-000000000002");
    /**
     * UUID for system message handler
     */
    public static final UUID ID_SYS_MSG = UUID.fromString("a1b248d1-949b-01e4-a84b-000000000003");
    /**
     * IP for loopback communication
     */
    public static final InetAddress IP_LOOPBACK;

    static {
        InetAddress tmp = null;
        try {
            tmp = InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
        } catch (UnknownHostException ex) {
            LOG.warning("Error obtaining loopback IP.");
            try {
                tmp = InetAddress.getLocalHost();
            } catch (UnknownHostException ex1) {
                LOG.severe("Error obtaining local IP.");
            }
        }
        IP_LOOPBACK = tmp;
    }

    private GlobalConstants() {
    }

}
