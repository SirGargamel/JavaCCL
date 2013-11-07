package cz.tul.javaccl;

import cz.tul.javaccl.socket.IDFilter;

/**
 * Interface allowing enabling / disabling of some library components.
 *
 * @author Petr Jecmen
 */
public interface ComponentManager {

    /**
     *
     * @param filter new filter of client IDs
     */
    void setIdFilter(final IDFilter filter);

    /**
     *
     * @param enable true to enable automatic server / client discovery
     */
    void enableDiscoveryDaemon(final boolean enable);

}
