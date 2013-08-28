package cz.tul.comm.socket;

import cz.tul.comm.communicator.Communicator;
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
