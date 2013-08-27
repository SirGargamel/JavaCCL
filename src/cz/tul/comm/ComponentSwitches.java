package cz.tul.comm;

/**
 * Switches for enabling / disabling optional parts of communication library.
 *
 * @author Petr Ječmen
 */
public class ComponentSwitches {

    /**
     * enable/disable discovering clients using UDP broadcast
     */
    public static boolean useClientDiscovery = true;
    /**
     * allow server registration after unsuccessfull server discovery daemon
     * initialization
     */
    public static boolean useClientAutoConnectLocalhost = true;

    private ComponentSwitches() {
    }
}
