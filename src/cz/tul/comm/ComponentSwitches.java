package cz.tul.comm;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    public static boolean useClientDiscovery = true;
    /**
     * allow server registration after unsuccessfull server discovery daemon
     * initialization
     */
    public static boolean useClientAutoConnectLocalhost = true;
    /**
     * enable/disable cycling update of client status (for non-working clients)
     */
    public static boolean useClientStatus = false;

    private ComponentSwitches() {
    }
}
