package cz.tul.javaccl;

import cz.tul.javaccl.socket.IDFilter;

/**
 *
 * @author Petr Jecmen
 */
public interface ComponentManager {

    void setIdFilter(final IDFilter filter);
    
    void enableDiscoveryDaemon(final boolean enable);
    
}
