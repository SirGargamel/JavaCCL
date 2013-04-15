package cz.tul.comm.server;

import cz.tul.comm.communicator.Communicator;
import java.net.InetAddress;
import java.util.Collection;
import java.util.UUID;

/**
 * Interface for managing clients.
 *
 * @author Petr Ječmen
 */
public interface IClientManager {

    /**
     * Register a new client.
     *
     * @param adress client IP
     * @param port client port
     * @return {@link Communicator} for client communication
     */
    Communicator registerClient(final InetAddress adress, final int port);

    /**
     * @param id client UUID
     */
    void deregisterClient(final UUID id);

    /**
     * @param adress client IP
     * @param port client port
     * @return {@link Communicator} for communication with given IP (if
     * registered)
     */
    Communicator getClient(final InetAddress adress, final int port);

    /**
     * @param id clients UUID
     * @return client communicator with given UUID
     */
    Communicator getClient(final UUID id);

    /**
     * @return list of all clients
     */
    Collection<Communicator> getClients();
}
