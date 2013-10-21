package cz.tul.javaccl.communicator;

import cz.tul.javaccl.exceptions.ConnectionException;
import java.net.InetAddress;
import java.util.UUID;

/**
 * Interface for contacting some another instance of JavaCCL entity (client or
 * server).
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
     * @return tre if the library can deliver messages to target
     */
    boolean isOnline();

    /**
     * @return last known client status
     */
    Status getStatus();

    /**
     * Send data to given target.
     *
     * @param data data for sending (must implement Serializable interface)
     * @return true for successfull send
     * @throws IllegalArgumentException Data could not be serialized.
     * @throws ConnectionException Target could not be contacted.
     */
    Object sendData(final Object data) throws IllegalArgumentException, ConnectionException;

    /**
     * Send data to given target.
     *
     * @param data data for sending (must implement Serializable interface)
     * @param timeout time, after which sending is considered unsuccessfull
     * @return true for successfull send
     * @throws IllegalArgumentException Data could not be serialized.
     * @throws ConnectionException Target could not be contacted.
     */
    Object sendData(final Object data, final int timeout) throws IllegalArgumentException, ConnectionException;
}
