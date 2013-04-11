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
    public static boolean useSettings = true;
    /**
     * enable/disable discovering clients using UDP broadcast
     */
    public static boolean useClientDiscovery = true;
    /**
     * enable/disable cycling update of client status (for non-working clients)
     */
    public static boolean useClientStatus = true;
    public static boolean useClientAutoConnectLocalhost = true;
    public static final boolean useFileLogger = true;
    public static final boolean useDebugMode = true;
    
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
