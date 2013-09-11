package cz.tul.javaccl.socket;

import cz.tul.javaccl.communicator.Communicator;
import java.util.Collection;

/**
 *
 * @author Petr Ječmen
 */
public interface ClientLister {
    
    /**     
     * @return coolection representing all registered clients
     */
    Collection<Communicator> getClients();
    
}
