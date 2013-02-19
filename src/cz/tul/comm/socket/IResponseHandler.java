package cz.tul.comm.socket;

import java.net.InetAddress;
import java.util.Queue;

/**
 * Interface for classes used to register listeners for client answers.
 * @author Petr Ječmen
 */
public interface IResponseHandler {

    void registerResponse(final InetAddress address, final Object owner);

    void deregisterResponse(final InetAddress address, final Object owner);

    Queue<Object> getResponseQueue(final Object owner);

}
