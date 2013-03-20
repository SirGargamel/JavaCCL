package cz.tul.comm.server;

import cz.tul.comm.communicator.Communicator;
import java.net.InetAddress;
import java.util.Set;

/**
 * Interface for managing clients.
 *
 * @author Petr Ječmen
 */
public interface IClientManager {

    /**
     * Deregister client with given IP.
     *
     * @param adress client IP
     */
    void deregisterClient(final InetAddress adress);

    /**
     * @param adress client IP
     * @return {@link Communicator} for communication with given IP (if
     * registered)
     */
    Communicator getClient(final InetAddress adress);

    /**
     * @return list of all clients
     */
    Set<Communicator> getClients();

    /**
     * Register a new client.
     *
     * @param adress client IP
     * @param port client port
     * @return {@link Communicator} for client communication
     */
    Communicator registerClient(final InetAddress adress, final int port);
}
