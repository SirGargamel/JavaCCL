package cz.tul.comm.communicator;

import cz.tul.comm.exceptions.ConnectionException;
import java.net.InetAddress;
import java.util.UUID;

/**
 *
 * @author Petr Ječmen
 */
public interface Communicator {

    /**
     * Check status of target.
     *
     * @return current status of target.
     */
    Status checkStatus();

    /**
     *
     * @return target IP address
     */
    InetAddress getAddress();

    /**
     * @return CommunicatorImpl ID (assigned by server)
     */
    UUID getTargetId();

    /**
     * @return target client port
     */
    int getPort();

    /**
     * @return last known client status
     */
    Status getStatus();

    /**
     * Send data to given target.
     *
     * @param data data for sending (must implement Serializable interface)
     * @return true for successfull send
     */
    Object sendData(final Object data) throws IllegalArgumentException, ConnectionException;

    /**
     * Send data to given target.
     *
     * @param data data for sending (must implement Serializable interface)
     * @param timeout time, after which sending is considered unsuccessfull
     * @return true for successfull send
     */
    Object sendData(final Object data, final int timeout) throws IllegalArgumentException, ConnectionException;
    
}
