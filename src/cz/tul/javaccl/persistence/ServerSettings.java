package cz.tul.javaccl.persistence;

import cz.tul.javaccl.GlobalConstants;
import cz.tul.javaccl.communicator.Communicator;
import cz.tul.javaccl.exceptions.ConnectionException;
import cz.tul.javaccl.server.ClientManager;
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
    private static final String FIELD_NAME_CLIENT = "client";

    /**
     * Read and use server settings.
     *
     * @param settingsFile target settings file
     * @param clientManager interface for managing clients
     * @return true for successfull load
     */
    public static boolean deserialize(final File settingsFile, final ClientManager clientManager) {
        log.log(Level.FINE, "Deserializing server settings.");
        boolean result = false;

        if (settingsFile.canRead()) {
            try {
                Map<String, String> fields = SimpleXMLSettingsFile.loadSimpleXMLFile(settingsFile);
                for (String f : fields.keySet()) {
                    if (f != null && f.equals(FIELD_NAME_CLIENT)) {
                        String[] split = fields.get(f).split(GlobalConstants.DELIMITER);
                        try {
                            if (clientManager.registerClient(InetAddress.getByName(split[0]), Integer.valueOf(split[1])) != null) {
                                result = true;
                            }
                        } catch (UnknownHostException ex) {
                            log.log(Level.WARNING, "Unkonwn host found in settings");
                            log.log(Level.FINE, "Unkonwn host found in settings", ex);
                        } catch (NumberFormatException ex) {
                            log.log(Level.WARNING, "Unkonwn host found in settings");
                            log.log(Level.FINE, "Unkonwn host found in settings", ex);
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            log.log(Level.WARNING, "Unkonwn host found in settings");
                            log.log(Level.FINE, "Unkonwn host found in settings", ex);
                        } catch (ConnectionException ex) {
                            log.log(Level.WARNING, "Could not connect client with IP " + split[0] + " on port " + split[1] + " - " + ex.getExceptionCause());
                        }
                    } else {
                        log.log(Level.FINE, "Unknown field - " + f);
                    }
                }
            } catch (IOException ex) {
                log.log(Level.WARNING, "Error accessing server settings at " + settingsFile.getAbsolutePath() + ".");
                log.log(Level.FINE, "Error accessing server settings at " + settingsFile.getAbsolutePath() + ".", ex);
            } catch (SAXException ex) {
                log.log(Level.WARNING, "Wrong format of input XML.");
                log.log(Level.FINE, "Wrong format of input XML.", ex);
            }
        }

        return result;
    }

    /**
     * Store server settings to disk.
     *
     * @param settingsFile target settings file
     * @param clientManager interface for managing clients
     * @return true for successfull save
     */
    public static boolean serialize(final File settingsFile, final ClientManager clientManager) {
        log.log(Level.FINE, "Serializing server settings.");
        final SimpleXMLSettingsFile xml = new SimpleXMLSettingsFile();

        final Collection<Communicator> comms = clientManager.getClients();
        StringBuilder sb = new StringBuilder();
        for (Communicator c : comms) {
            sb.append(c.getAddress().getHostAddress());
            sb.append(GlobalConstants.DELIMITER);
            sb.append(c.getPort());
            xml.addField(FIELD_NAME_CLIENT, sb.toString());
            sb.setLength(0);
        }

        boolean result = false;
        try {
            result = xml.storeXML(settingsFile);
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error accessing server settings at " + settingsFile.getAbsolutePath() + ".");
            log.log(Level.FINE, "Error accessing server settings at " + settingsFile.getAbsolutePath() + ".", ex);
        }

        return result;
    }

    private ServerSettings() {
    }
}
