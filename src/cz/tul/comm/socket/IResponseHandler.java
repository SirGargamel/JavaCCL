package cz.tul.comm.socket;

import java.net.InetAddress;
import java.util.Queue;

/**
 *
 * @author Petr Ječmen
 */
public interface IResponseHandler {

    void registerResponse(final InetAddress address, final Object owner);

    void deregisterResponse(final InetAddress address, final Object owner);

    Queue<Object> getResponseQueue(final Object owner);

}
