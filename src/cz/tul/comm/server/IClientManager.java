package cz.tul.comm.server;

import cz.tul.comm.communicator.Communicator;
import java.net.InetAddress;
import java.util.Set;
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
     * Deregister client with given IP.
     *
     * @param adress client IP
     * @param port client port
     */
    void deregisterClient(final InetAddress adress, final int port);

    /**
     * @param adress client IP
     * @param port client port
     * @return {@link Communicator} for communication with given IP (if
     * registered)
     */
    Communicator getClient(final InetAddress adress, final int port);
    
    Communicator getClient(final UUID id);

    /**
     * @return list of all clients
     */
    Set<Communicator> getClients();
}
