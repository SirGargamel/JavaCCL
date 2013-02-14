package cz.tul.comm.socket;

import java.net.InetAddress;

/**
 *
 * @author Petr Ječmen
 */
public interface IMessageHandler {

    public void handleMessage(final InetAddress adress, final Object msg);

}
