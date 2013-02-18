package cz.tul.comm.socket;

import java.net.InetAddress;

/**
 *
 * @author Petr Ječmen
 */
public interface IResponseHandler {

    void registerResponse(final InetAddress address, final Object owner);

    void deregisterResponse(final InetAddress address, final Object owner);

    Object pickupResponse(final Object owner);

}
