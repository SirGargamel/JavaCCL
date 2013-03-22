package cz.tul.comm.client;

import java.net.InetAddress;

/**
 *
 * @author Petr Ječmen
 */
public interface IServerRegistrator {

    void registerServer(final InetAddress address, final int port);

    boolean isServerUp();
}
