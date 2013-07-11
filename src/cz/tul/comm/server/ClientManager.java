package cz.tul.comm.server;

import cz.tul.comm.socket.ClientLister;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.exceptions.ConnectionException;
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
     * @return {@link CommunicatorImpl} for client communication
     */
    Communicator registerClient(final InetAddress adress, final int port) throws IllegalArgumentException, ConnectionException;
    
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
