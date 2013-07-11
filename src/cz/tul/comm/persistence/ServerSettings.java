package cz.tul.comm.persistence;

import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.exceptions.ConnectionException;
import cz.tul.comm.server.ClientManager;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xml.sax.SAXException;

/**
 * Settings for server part.
 *
 * @author Petr Ječmen
 */
public class ServerSettings implements Serializable {

    private static final Logger log = Logger.getLogger(ServerSettings.class.getName());
    private static final String SERIALIZATION_NAME = "serverSettings.xml";
    private static final String IP_PORT_SPLITTER = ":";
    private static final String FIELD_NAME_CLIENT = "client";

    /**
     * Read and use server settings.
     *
     * @param clientManager interface for managing clients
     * @return true for successfull load
     */
    public static boolean deserialize(final ClientManager clientManager) {
        log.log(Level.CONFIG, "Deserializing server settings.");
        boolean result = true;

        File s = new File(SERIALIZATION_NAME);
        if (s.exists()) {
            try {
                Map<String, String> fields = SimpleXMLFile.loadSimpleXMLFile(s);
                for (String f : fields.keySet()) {
                    switch (f) {
                        case FIELD_NAME_CLIENT:
                            String[] split = fields.get(f).split(IP_PORT_SPLITTER);
                            try {

                                clientManager.registerClient(InetAddress.getByName(split[0]), Integer.valueOf(split[1]));
                            } catch (UnknownHostException | NumberFormatException | ArrayIndexOutOfBoundsException ex) {
                                result = false;
                                log.log(Level.WARNING, "Unkonwn host found in settings", ex);
                            } catch (ConnectionException ex) {
                                log.log(Level.WARNING, "Could not connect client with IP {0} on port {1}.", new Object[]{split[0], split[1]});
                            }
                            break;
                        default:
                            log.log(Level.CONFIG, "Unknown field - {0}", f);
                            break;
                    }
                }
            } catch (IOException ex) {
                result = false;
                log.log(Level.WARNING, "Error accessing server settings at " + SERIALIZATION_NAME + ".", ex);
            } catch (SAXException ex) {
                result = false;
                log.log(Level.WARNING, "Wrong format of input XML.", ex);
            }
        } else {
            result = false;
        }

        return result;
    }

    /**
     * Store server settings to disk.
     *
     * @param clientManager interface for managing clients
     * @return true for successfull save
     */
    public static boolean serialize(final ClientManager clientManager) {
        log.log(Level.CONFIG, "Serializing server settings.");
        final SimpleXMLFile xml = new SimpleXMLFile();

        final Collection<Communicator> comms = clientManager.getClients();
        StringBuilder sb = new StringBuilder();
        for (Communicator c : comms) {
            sb.append(c.getAddress().getHostAddress());
            sb.append(IP_PORT_SPLITTER);
            sb.append(c.getPort());
            xml.addField(FIELD_NAME_CLIENT, sb.toString());
            sb.setLength(0);
        }

        boolean result = false;
        try {
            result = xml.storeXML(new File(SERIALIZATION_NAME));
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error accessing server settings at " + SERIALIZATION_NAME + ".", ex);
        }

        return result;
    }

    private ServerSettings() {
    }
}
