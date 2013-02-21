package cz.tul.comm.socket;

import java.net.InetAddress;
import java.util.Queue;
import java.util.UUID;

/**
 * Interface for classes used to register listeners for client answers.
 * @author Petr Ječmen
 */
public interface IResponseHandler {

    void registerResponse(final InetAddress address, final Object owner);

    void registerResponse(final UUID id, final Object owner);

    void deregisterResponse(final InetAddress address, final Object owner);

    void deregisterResponse(final UUID id, final Object owner);

    Queue<Object> getResponseQueue(final Object owner);

}
