package cz.tul.comm.server;

import cz.tul.comm.client.Comm_Client;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Settings for server part.
 * @author Petr Ječmen
 */
final class Settings implements Serializable {

    static final String SERIALIZATION_NAME = "serverSettings.xml";
    private static final long serialVersionUID = 2L;
    private Set<String> clients;
    private int defaultClientPort;

    Settings() {
        clients = new HashSet<>();
        defaultClientPort = Comm_Client.PORT;
    }

    Set<String> getClients() {
        return clients;
    }

    void setClients(Set<String> clients) {
        this.clients = clients;
    }

    int getDefaultClientPort() {
        return defaultClientPort;
    }

    void setDefaultClientPort(int defaultClientPort) {
        this.defaultClientPort = defaultClientPort;
    }

}
