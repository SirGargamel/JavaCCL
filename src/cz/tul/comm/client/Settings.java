package cz.tul.comm.client;

import java.io.Serializable;
import java.net.InetAddress;

/**
 *
 * @author Petr Ječmen
 */
public class Settings implements Serializable {

    public static final String SERIALIZATION_NAME = "clientSettings.xml";
    private static final long serialVersionUID = 1L;
    private InetAddress serverAdress;

    public Settings() {
        serverAdress = InetAddress.getLoopbackAddress();
    }

    public InetAddress getServerAdress() {
        return serverAdress;
    }

    public void setServerAdress(InetAddress serverAdress) {
        this.serverAdress = serverAdress;
    }
}
