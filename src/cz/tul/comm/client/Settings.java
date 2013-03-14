package cz.tul.comm.client;

import cz.tul.comm.server.Comm_Server;
import java.io.Serializable;
import java.net.InetAddress;

/**
 * Settings for client part. Default settings for server address is
 * loopback adress (eg. server and client are run on same machine).
 * @author Petr Ječmen
 */
final class Settings implements Serializable {

    static final String SERIALIZATION_NAME = "clientSettings.xml";
    private static final long serialVersionUID = 2L;
    private static final String DEFAULT_IP = InetAddress.getLoopbackAddress().getHostAddress();
    private String serverAdress;
    private int serverPort;

    Settings() {
        serverAdress = DEFAULT_IP;
        serverPort = Comm_Server.PORT;
    }

    String getServerAdress() {
        return serverAdress;
    }

    void setServerAdress(String serverAdress) {
        this.serverAdress = serverAdress;
    }

    int getServerPort() {
        return serverPort;
    }

    void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }
}
