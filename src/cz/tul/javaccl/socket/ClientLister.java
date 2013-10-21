package cz.tul.javaccl.socket;

import cz.tul.javaccl.communicator.Communicator;
import java.util.Collection;

/**
 * Interface for obtaining list of clients.
 *
 * @author Petr Ječmen
 */
public interface ClientLister {

    /**
     * @return coolection representing all registered clients
     */
    Collection<Communicator> getClients();

}
