package cz.tul.comm.socket;

import java.net.InetAddress;

/**
 * Interface for classes able to parse received message from a client.
 * @author Petr Ječmen
 */
public interface IDataHandler {

    public void handleData(final InetAddress adress, final Object data);

}
