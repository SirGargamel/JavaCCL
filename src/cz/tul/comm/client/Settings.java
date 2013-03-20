package cz.tul.comm.client;

import cz.tul.comm.server.Comm_Server;
import java.io.Serializable;
import java.net.InetAddress;

/**
 * Settings for client part. Default settings for server address is
 * loopback adress (eg. server and client are run on same machine).
 * @author Petr Ječmen
 */
public final class Settings implements Serializable {

    static final String SERIALIZATION_NAME = "clientSettings.xml";
    private static final long serialVersionUID = 2L;
    private static final String DEFAULT_IP = InetAddress.getLoopbackAddress().getHostAddress();
    private String serverAdress;
    private int serverPort;

    public Settings() {
        serverAdress = DEFAULT_IP;
        serverPort = Comm_Server.PORT;
    }

    public String getServerAdress() {
        return serverAdress;
    }

    public void setServerAdress(String serverAdress) {
        this.serverAdress = serverAdress;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }
}
