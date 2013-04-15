package cz.tul.comm.client;

import cz.tul.comm.communicator.Communicator;
import java.net.InetAddress;

/**
 * Registrator of server IP and port. Offers also functionality to test if the
 * server is reachable.
 *
 * @author Petr Ječmen
 */
public interface IServerInterface {

    /**
     * @param address server IP
     * @param port server port
     */
    boolean registerToServer(final InetAddress address, final int port);

    /**
     * Send server info that client no longer wants to participate.
     */
    void deregisterFromServer();

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
}
