package cz.tul.javaccl.persistence;

import cz.tul.javaccl.Constants;
import cz.tul.javaccl.client.ServerInterface;
import cz.tul.javaccl.communicator.Communicator;
import cz.tul.javaccl.exceptions.ConnectionException;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xml.sax.SAXException;

/**
 * Settings for client part. Default settings for server address is loopback
 * adress (eg. server and client are run on same machine) on default server
 * port.
 *
 * @author Petr Ječmen
 */
public class ClientSettings {

    private static final Logger log = Logger.getLogger(ClientSettings.class.getName());
    private static final String FIELD_NAME_SERVER = "server";

    /**
     * Save client settings to disk.
     *
     * @param settingsFile target settings file
     * @param reg server parameters registrator
     * @return true for successfull deserialization
     */
    public static boolean deserialize(final File settingsFile, final ServerInterface reg) {
        log.log(Level.FINE, "Deserializing client settings.");
        boolean result = false;

        if (settingsFile.canRead()) {
            InetAddress ip = null;
            int port = Constants.DEFAULT_PORT;
            try {
                Map<String, String> fields = SimpleXMLSettingsFile.loadSimpleXMLFile(settingsFile);
                for (String f : fields.keySet()) {
                    if (f != null && f.equals(FIELD_NAME_SERVER)) {
                        try {
                            String[] split = fields.get(f).split(Constants.DELIMITER);
                            ip = InetAddress.getByName(split[0]);
                            if (split.length > 1) {
                                port = Integer.valueOf(split[1]);
                            }
                        } catch (UnknownHostException ex) {
                            result = false;
                            log.log(Level.WARNING, "Unkonwn server ip found in settings");
                            log.log(Level.FINE, "Unkonwn server ip found in settings", ex);
                        } catch (NumberFormatException ex) {
                            result = false;
                            log.log(Level.WARNING, "Unkonwn server port found in settings");
                            log.log(Level.FINE, "Unkonwn server port found in settings", ex);
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            result = false;
                            log.log(Level.WARNING, "Unkonwn server info found in settings.");
                            log.log(Level.FINE, "Unkonwn server info found in settings.", ex);
                        }
                    } else {
                        log.log(Level.FINE, "Unknown field - " + f);
                    }
                }

                if (ip != null) {
                    try {
                        if (reg.registerToServer(ip, port)) {
                            result = true;
                        }
                    } catch (ConnectionException ex) {
                        log.log(Level.WARNING, "Could not connect to server with " + ip.getHostAddress() + ", port " + port);
                    }
                }
            } catch (IOException ex) {
                log.log(Level.WARNING, "Error accessing client settings at " + settingsFile.getAbsolutePath() + ".");
                log.log(Level.FINE, "Error accessing client settings at " + settingsFile.getAbsolutePath() + ".", ex);
            } catch (SAXException ex) {
                log.log(Level.WARNING, "Wrong format of input XML.");
                log.log(Level.FINE, "Wrong format of input XML.", ex);
            }
        }

        return result;
    }

    /**
     * Load client settings from disk.
     *
     * @param settingsFile target settings file
     * @param serverCommunicator server communicator
     * @return true for successfull save
     */
    public static boolean serialize(final File settingsFile, final Communicator serverCommunicator) {
        log.log(Level.FINE, "Serializing client settings.");

        SimpleXMLSettingsFile xml = new SimpleXMLSettingsFile();

        xml.addField(
                FIELD_NAME_SERVER,
                composeServerAddress(serverCommunicator.getAddress(), serverCommunicator.getPort()));

        boolean result = false;
        try {
            result = xml.storeXML(settingsFile);
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error accessing client settings at " + settingsFile.getAbsolutePath() + ".");
            log.log(Level.FINE, "Error accessing client settings at " + settingsFile.getAbsolutePath() + ".", ex);
        }

        return result;
    }

    private static String composeServerAddress(final InetAddress address, final int port) {
        StringBuilder sb = new StringBuilder();
        sb.append(address.getHostAddress());
        sb.append(Constants.DELIMITER);
        sb.append(port);
        return sb.toString();
    }

    private ClientSettings() {
    }
}
