package cz.tul.comm.client;

import java.io.Serializable;
import java.net.InetAddress;

/**
 * Settings for client part. Default settings for server address is
 * loopback adress (eg. server and client are run on same machine).
 * @author Petr Ječmen
 */
public class Settings implements Serializable {

    public static final String SERIALIZATION_NAME = "clientSettings.xml";
    private static final long serialVersionUID = 2L;
    private static final String DEFAULT_IP = InetAddress.getLoopbackAddress().getHostAddress();
    private String serverAdress;

    public Settings() {
        serverAdress = DEFAULT_IP;
    }

    public String getServerAdress() {
        return serverAdress;
    }

    public void setServerAdress(String serverAdress) {
        this.serverAdress = serverAdress;
    }
}
