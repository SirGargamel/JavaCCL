package cz.tul.comm.client;

import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.exceptions.ConnectionException;
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
     */
    boolean registerToServer(final InetAddress address, final int port) throws ConnectionException;
    
    void setServerInfo(final InetAddress address, final int port, final UUID clientId);

    /**
     * Send server info that client no longer wants to participate.
     */
    void deregisterFromServer() throws ConnectionException;

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
    
    int getLocalSocketPort();
}
