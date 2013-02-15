package cz.tul.comm.server;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Petr Ječmen
 */
public class Settings implements Serializable {

    public static final String SERIALIZATION_NAME = "serverSettings.xml";
    private static final long serialVersionUID = 1L;
    private Set<InetAddress> clients;

    public Settings() {
        clients = new HashSet<>();
    }

    public Set<InetAddress> getClients() {
        return clients;
    }

    public void setClients(Set<InetAddress> clients) {
        this.clients = clients;
    }

}
