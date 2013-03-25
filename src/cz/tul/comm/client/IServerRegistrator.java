package cz.tul.comm.client;

import java.net.InetAddress;

/**
 * Registrator of server IP and port. Offers also functionality to test if the
 * server is reachable.
 *
 * @author Petr Ječmen
 */
public interface IServerRegistrator {

    /**
     * @param address server IP
     * @param port server port
     */
    void registerServer(final InetAddress address, final int port);

    /**
     * Test if the server reachable.
     *
     * @return true if the server is aviable and responding
     */
    boolean isServerUp();
}
