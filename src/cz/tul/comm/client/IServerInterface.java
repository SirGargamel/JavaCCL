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
    void registerServer(final InetAddress address, final int port);

    void deregisterFromServer();

    /**
     * Test if the server reachable.
     *
     * @return true if the server is aviable and responding
     */
    boolean isServerUp();    

    Communicator getServerComm();
}
