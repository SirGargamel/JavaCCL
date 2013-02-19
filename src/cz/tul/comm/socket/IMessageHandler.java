package cz.tul.comm.socket;

import java.net.InetAddress;

/**
 * Interface for classes able to parse received message from a client.
 * @author Petr Ječmen
 */
public interface IMessageHandler {

    public void handleMessage(final InetAddress adress, final Object msg);

}
