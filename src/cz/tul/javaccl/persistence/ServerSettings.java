package cz.tul.javaccl.persistence;

import cz.tul.javaccl.communicator.Communicator;
import cz.tul.javaccl.exceptions.ConnectionException;
import cz.tul.javaccl.server.ClientManager;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xml.sax.SAXException;

/**
 * Settings for SERVER part.
 *
 * @author Petr Ječmen
 */
public final class ServerSettings implements Serializable {

    private static final Logger LOG = Logger.getLogger(ServerSettings.class.getName());
    private static final String DELIMITER = "-";

    /**
     * Read and use SERVER settings.
     *
     * @param settingsFile target settings file
     * @param clientManager interface for managing clients
     * @return true for successfull load
     */
    public static boolean deserialize(final File settingsFile, final ClientManager clientManager) {
        LOG.log(Level.FINE, "Deserializing server settings.");
        boolean result = false;

        if (settingsFile.canRead()) {
            try {
                final List<Map.Entry<String, String>> fields = SimpleXMLSettingsFile.loadSimpleXMLFile(settingsFile);
                String fieldName;
                for (Entry<String, String> e : fields) {
                    fieldName = e.getKey();
                    if (fieldName != null && fieldName.equals(XmlNodes.CLIENT.toString())) {
                        final String[] split = e.getValue().split(DELIMITER);
                        try {
                            if (clientManager.registerClient(InetAddress.getByName(split[0]), Integer.parseInt(split[1])) != null) {
                                result = true;
                            }
                        } catch (UnknownHostException ex) {
                            LOG.log(Level.WARNING, "Unkonwn host found in settings");
                            LOG.log(Level.FINE, "Unkonwn host found in settings", ex);
                        } catch (NumberFormatException ex) {
                            LOG.log(Level.WARNING, "Unkonwn host found in settings");
                            LOG.log(Level.FINE, "Unkonwn host found in settings", ex);
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            LOG.log(Level.WARNING, "Unkonwn host found in settings");
                            LOG.log(Level.FINE, "Unkonwn host found in settings", ex);
                        } catch (ConnectionException ex) {
                            LOG.log(Level.WARNING, "Could not connect client with IP {0} on port {1} - {2}", new Object[]{split[0], split[1], ex.getExceptionCause()});
                        }
                    } else {
                        LOG.log(Level.FINE, "Unknown field - {0}", fieldName);
                    }
                }
            } catch (IOException ex) {
                LOG.log(Level.WARNING, "Error accessing server settings at {0}.", settingsFile.getAbsolutePath());
                LOG.log(Level.FINE, "Error accessing server settings at " + settingsFile.getAbsolutePath() + ".", ex);
            } catch (SAXException ex) {
                LOG.log(Level.WARNING, "Wrong format of input XML.");
                LOG.log(Level.FINE, "Wrong format of input XML.", ex);
            }
        }

        return result;
    }

    /**
     * Store SERVER settings to disk.
     *
     * @param settingsFile target settings file
     * @param clientManager interface for managing clients
     * @return true for successfull save
     */
    public static boolean serialize(final File settingsFile, final ClientManager clientManager) {
        LOG.log(Level.FINE, "Serializing server settings.");
        final SimpleXMLSettingsFile xml = new SimpleXMLSettingsFile();

        final Collection<Communicator> comms = clientManager.getClients();
        final StringBuilder sb = new StringBuilder();
        for (Communicator c : comms) {
            sb.append(c.getAddress().getHostAddress());
            sb.append(DELIMITER);
            sb.append(c.getPort());
            xml.addField(XmlNodes.CLIENT.toString(), sb.toString());
            sb.setLength(0);
        }

        boolean result = false;
        try {
            result = xml.storeXML(settingsFile);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Error accessing server settings at {0}.", settingsFile.getAbsolutePath());
            LOG.log(Level.FINE, "Error accessing server settings at " + settingsFile.getAbsolutePath() + ".", ex);
        }

        return result;
    }

    private ServerSettings() {
    }
}
