package cz.tul.javaccl.client;

import cz.tul.javaccl.communicator.Communicator;
import cz.tul.javaccl.exceptions.ConnectionException;
import java.net.InetAddress;
import java.util.UUID;

/**
 * Registrator of server IP and port. Offers also functionality to test if the
 * server is reachable.
 *
 * @author Petr Ječmen
 */
public interface ServerInterface {

    /**
     * @param address server IP
     * @param port server port
     * @return true for successfull registration
     * @throws ConnectionException Server could not be contacted
     */
    boolean registerToServer(final InetAddress address, final int port) throws ConnectionException;

    /**
     * Set the server params without contacting him
     *
     * @param address IP of the server
     * @param port port of the server
     * @param clientId local client UUID
     */
    void setServerInfo(final InetAddress address, final int port, final UUID clientId);

    /**
     * Send server info that client no longer wants to participate.
     *
     * @throws ConnectionException
     */
    void deregisterFromServer() throws ConnectionException;

    /**
     * Block any further connections to current server.
     */
    void disconnectFromServer();

    /**
     * Test if the server reachable.
     *
     * @return true if the server is aviable and responding
     */
    boolean isServerUp();

    /**
     * @return server communicator
     */
    Communicator getServerComm();

    /**
     * @return local port on which te client runs
     */
    int getLocalSocketPort();
}
