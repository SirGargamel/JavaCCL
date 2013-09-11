package cz.tul.javaccl.server;

import cz.tul.javaccl.socket.ClientLister;
import cz.tul.javaccl.communicator.Communicator;
import cz.tul.javaccl.exceptions.ConnectionException;
import java.net.InetAddress;
import java.util.UUID;

/**
 * Interface for managing clients.
 *
 * @author Petr Ječmen
 */
public interface ClientManager extends ClientLister {

    /**
     * Register a new client.
     *
     * @param adress client IP
     * @param port client port
     * @return {@link Communicator} for client communication
     * @throws IllegalArgumentException Invalid IP or port
     * @throws ConnectionException Could not connect to the client
     */
    Communicator registerClient(final InetAddress adress, final int port) throws IllegalArgumentException, ConnectionException;

    /**
     * Add client to local client registry without making a connection to client
     * and assigning new UUID.
     *
     * @param address target IP
     * @param port target port
     * @param clientId clients UUID
     * @return {@link Communicator} for client communication
     */
    Communicator addClient(final InetAddress address, final int port, final UUID clientId);

    /**
     * Deregister client with given id.
     *
     * @param id client UUID
     */
    void deregisterClient(final UUID id);

    /**
     * @param adress client IP
     * @param port client port
     * @return {@link CommunicatorImpl} for communication with given IP (if
     * registered)
     */
    Communicator getClient(final InetAddress adress, final int port);

    /**
     * @param id clients UUID
     * @return client communicator with given UUID
     */
    Communicator getClient(final UUID id);
}
