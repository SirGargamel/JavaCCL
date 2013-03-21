package cz.tul.comm.persistence;

import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.gui.UserLogging;
import cz.tul.comm.server.IClientManager;
import java.io.File;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ServerSettings for server part.
 *
 * @author Petr Ječmen
 */
public final class ServerSettings implements Serializable {

    private static final Logger log = Logger.getLogger(ServerSettings.class.getName());
    private static final String SERIALIZATION_NAME = "serverSettings.xml";
    private static final String IP_PORT_SPLITTER = ":";
    private static final long serialVersionUID = 3L;
    private Set<String> clients;

    public ServerSettings() {
        clients = new HashSet<>();
    }

    public Set<String> getClients() {
        return clients;
    }

    public void setClients(Set<String> clients) {
        this.clients = clients;
    }

    public static boolean deserialize(final IClientManager clientManager) {
        boolean result = true;
        File s = new File(SERIALIZATION_NAME);
        if (s.exists()) {
            Object in = SerializationUtils.loadXMLItemFromDisc(s);
            if (in instanceof ServerSettings) {
                ServerSettings settings = (ServerSettings) in;
                for (String a : settings.clients) {
                    try {
                        String[] split = a.split(IP_PORT_SPLITTER);
                        clientManager.registerClient(InetAddress.getByName(split[0]), Integer.valueOf(split[1]));
                    } catch (UnknownHostException | NumberFormatException | ArrayIndexOutOfBoundsException ex) {
                        UserLogging.showWarningToUser("Unknown host found in settings - " + ex.getLocalizedMessage());
                        log.log(Level.WARNING, "Unkonwn host found in settings", ex);
                    }
                }
            } else {
                result = false;
            }
        } else {
            result = false;
        }

        return result;
    }

    public static boolean serialize(final IClientManager clientManager) {
        final ServerSettings s = new ServerSettings();
        final Set<String> clients = s.clients;
        clients.clear();

        Set<Communicator> comms = clientManager.getClients();

        StringBuilder sb = new StringBuilder();
        for (Communicator c : comms) {
            sb.append(c.getAddress().getHostAddress());
            sb.append(IP_PORT_SPLITTER);
            sb.append(c.getPort());
            clients.add(sb.toString());
            sb.setLength(0);
        }

        return SerializationUtils.saveItemToDiscAsXML(new File(SERIALIZATION_NAME), s);
    }
}
