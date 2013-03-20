package cz.tul.comm.server;

import cz.tul.comm.client.Comm_Client;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Settings for server part.
 * @author Petr Ječmen
 */
public final class Settings implements Serializable {

    static final String SERIALIZATION_NAME = "serverSettings.xml";
    private static final long serialVersionUID = 2L;
    private Set<String> clients;
    private int defaultClientPort;

    public Settings() {
        clients = new HashSet<>();
        defaultClientPort = Comm_Client.PORT;
    }

    public Set<String> getClients() {
        return clients;
    }

    public void setClients(Set<String> clients) {
        this.clients = clients;
    }

    public int getDefaultClientPort() {
        return defaultClientPort;
    }

    public void setDefaultClientPort(int defaultClientPort) {
        this.defaultClientPort = defaultClientPort;
    }

}
