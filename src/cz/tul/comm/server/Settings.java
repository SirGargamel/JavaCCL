package cz.tul.comm.server;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Settings for server part.
 * @author Petr Ječmen
 */
public class Settings implements Serializable {

    public static final String SERIALIZATION_NAME = "serverSettings.xml";
    private static final long serialVersionUID = 2L;
    private Set<String> clients;

    public Settings() {
        clients = new HashSet<>();
    }

    public Set<String> getClients() {
        return clients;
    }

    public void setClients(Set<String> clients) {
        this.clients = clients;
    }

}
