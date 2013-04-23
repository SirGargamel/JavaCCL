package cz.tul.comm;

import java.util.logging.Level;

/**
 * Switches for enabling / disabling optional parts of communication library.
 *
 * @author Petr Ječmen
 */
public class ComponentSwitches {

    /**
     * enable/disable loading/saving settings
     */
    public static boolean useSettings = false;
    /**
     * enable/disable discovering clients using UDP broadcast
     */
    public static boolean useClientDiscovery = false;
    /**
     * allow server registration after unsuccessfull server discovery daemon
     * initialization
     */
    public static boolean useClientAutoConnectLocalhost = true;
    /**
     * enable/disable cycling update of client status (for non-working clients)
     */
    public static boolean useClientStatus = false;
    /**
     * enable/disable loggin to file
     */
    public static final boolean useFileLogger = true;
    /**
     * logging level
     */
    public static final boolean useDebugMode = false;

    static {
        if (useFileLogger) {
            Utils.prepareFileLogger();
        }
        if (useDebugMode) {
            Utils.adjustMainLoggerLevel(Level.FINE);
        }
    }

    private ComponentSwitches() {
    }
}
