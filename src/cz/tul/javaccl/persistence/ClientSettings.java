package cz.tul.javaccl.persistence;

import cz.tul.javaccl.GlobalConstants;
import cz.tul.javaccl.client.ServerInterface;
import cz.tul.javaccl.communicator.Communicator;
import cz.tul.javaccl.exceptions.ConnectionException;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xml.sax.SAXException;

/**
 * Settings for CLIENT part. Default settings for SERVER address is loopback
 adress (eg. SERVER and CLIENT are run on same machine) on default SERVER
 port.
 *
 * @author Petr Ječmen
 */
public final class ClientSettings {
    
    private static final Logger LOG = Logger.getLogger(ClientSettings.class.getName());
    private static final String DELIMITER = "-";

    /**
     * Save CLIENT settings to disk.
     *
     * @param settingsFile target settings file
     * @param reg SERVER parameters registrator
     * @return true for successfull deserialization
     */
    public static boolean deserialize(final File settingsFile, final ServerInterface reg) {
        LOG.log(Level.FINE, "Deserializing client settings.");
        boolean result = false;

        if (settingsFile.canRead()) {
            InetAddress ip = null;
            int port = GlobalConstants.DEFAULT_PORT;
            try {
                final List<Map.Entry<String, String>> fields = SimpleXMLSettingsFile.loadSimpleXMLFile(settingsFile);
                String fieldName;
                for (Map.Entry<String, String> e : fields) {
                    fieldName = e.getKey();
                    if (!result && fieldName != null && fieldName.equals(XmlNodes.SERVER.toString())) {
                        try {
                            final String[] split = e.getValue().split(DELIMITER);
                            ip = InetAddress.getByName(split[0]);
                            if (split.length > 1) {
                                port = Integer.parseInt(split[1]);
                            }
                        } catch (UnknownHostException ex) {
                            result = false;
                            LOG.log(Level.WARNING, "Unkonwn server ip found in settings");
                            LOG.log(Level.FINE, "Unkonwn server ip found in settings", ex);
                        } catch (NumberFormatException ex) {
                            result = false;
                            LOG.log(Level.WARNING, "Unkonwn server port found in settings");
                            LOG.log(Level.FINE, "Unkonwn server port found in settings", ex);
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            result = false;
                            LOG.log(Level.WARNING, "Unkonwn server info found in settings.");
                            LOG.log(Level.FINE, "Unkonwn server info found in settings.", ex);
                        }
                    } else {
                        LOG.log(Level.FINE, "Unknown field - {0}", fieldName);
                    }
                }

                if (ip != null) {
                    try {
                        if (reg.registerToServer(ip, port)) {
                            result = true;
                        }
                    } catch (ConnectionException ex) {
                        LOG.log(Level.WARNING, "Could not connect to server with {0}, port {1}", new Object[]{ip.getHostAddress(), port});
                    }
                }
            } catch (IOException ex) {
                LOG.log(Level.WARNING, "Error accessing client settings at {0}.", settingsFile.getAbsolutePath());
                LOG.log(Level.FINE, "Error accessing client settings at " + settingsFile.getAbsolutePath() + ".", ex);
            } catch (SAXException ex) {
                LOG.log(Level.WARNING, "Wrong format of input XML.");
                LOG.log(Level.FINE, "Wrong format of input XML.", ex);
            }
        }

        return result;
    }

    /**
     * Load CLIENT settings from disk.
     *
     * @param settingsFile target settings file
     * @param serverCommunicator SERVER communicator
     * @return true for successfull save
     */
    public static boolean serialize(final File settingsFile, final Communicator serverCommunicator) {
        LOG.log(Level.FINE, "Serializing client settings.");

        final SimpleXMLSettingsFile xml = new SimpleXMLSettingsFile();

        xml.addField(XmlNodes.SERVER.toString(),
                composeServerAddress(serverCommunicator.getAddress(), serverCommunicator.getPort()));

        boolean result = false;
        try {
            result = xml.storeXML(settingsFile);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Error accessing client settings at {0}.", settingsFile.getAbsolutePath());
            LOG.log(Level.FINE, "Error accessing client settings at " + settingsFile.getAbsolutePath() + ".", ex);
        }

        return result;
    }

    public static String composeServerAddress(final InetAddress address, final int port) {
        final StringBuilder sb = new StringBuilder();
        sb.append(address.getHostAddress());
        sb.append(DELIMITER);
        sb.append(port);
        return sb.toString();
    }

    private ClientSettings() {
    }
}
