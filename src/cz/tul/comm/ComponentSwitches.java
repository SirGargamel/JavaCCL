package cz.tul.comm;

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

    private ComponentSwitches() {
    }
}
