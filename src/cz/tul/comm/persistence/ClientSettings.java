package cz.tul.comm.persistence;

import cz.tul.comm.Constants;
import cz.tul.comm.client.ServerInterface;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.exceptions.ConnectionException;
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
    private static final String IP_PORT_SPLITTER = ":";
    private static final String FIELD_NAME_SERVER = "server";

    /**
     * Save client settings to disk.
     *
     * @param reg server parameters registrator
     * @return true for successfull deserialization
     */
    public static boolean deserialize(final File settingsFile, final ServerInterface reg) {
        log.log(Level.CONFIG, "Deserializing client settings.");
        boolean result = false;

        if (settingsFile.canRead()) {
            InetAddress ip = null;
            int port = Constants.DEFAULT_PORT;
            try {
                Map<String, String> fields = SimpleXMLSettingsFile.loadSimpleXMLFile(settingsFile);
                for (String f : fields.keySet()) {
                    switch (f) {
                        case FIELD_NAME_SERVER:
                            try {
                                String[] split = fields.get(f).split(IP_PORT_SPLITTER);
                                ip = InetAddress.getByName(split[0]);
                                if (split.length > 1) {
                                    port = Integer.valueOf(split[1]);
                                }
                            } catch (UnknownHostException ex) {
                                result = false;
                                log.log(Level.WARNING, "Unkonwn server ip found in settings", ex);
                            } catch (NumberFormatException ex) {
                                result = false;
                                log.log(Level.WARNING, "Unkonwn server port found in settings", ex);
                            } catch (ArrayIndexOutOfBoundsException ex) {
                                result = false;
                                log.log(Level.WARNING, "Unkonwn server info found in settings.", ex);
                            }
                            break;
                        default:
                            log.log(Level.FINE, "Unknown field - {0}", f);
                            break;
                    }
                }

                if (ip != null) {
                    try {
                        if (reg.registerToServer(ip, port)) {
                            result = true;
                        }
                    } catch (ConnectionException ex) {
                        log.log(Level.WARNING, "Could not connect to server with {0}, port {1}.", new Object[]{ip.getHostAddress(), port});
                    }
                }
            } catch (IOException ex) {
                log.log(Level.WARNING, "Error accessing client settings at " + settingsFile.getAbsolutePath() + ".", ex);
            } catch (SAXException ex) {
                log.log(Level.WARNING, "Wrong format of input XML.", ex);
            }
        }

        return result;
    }

    /**
     * Load client settings from disk.
     *
     * @param serverCommunicator server communicator
     * @return true for successfull save
     */
    public static boolean serialize(final File settingsFile, final Communicator serverCommunicator) {
        log.log(Level.CONFIG, "Serializing client settings.");

        SimpleXMLSettingsFile xml = new SimpleXMLSettingsFile();

        xml.addField(
                FIELD_NAME_SERVER,
                composeServerAddress(serverCommunicator.getAddress(), serverCommunicator.getPort()));

        boolean result = false;
        try {
            result = xml.storeXML(settingsFile);
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error accessing client settings at " + settingsFile.getAbsolutePath() + ".", ex);
        }

        return result;
    }

    private static String composeServerAddress(final InetAddress address, final int port) {
        StringBuilder sb = new StringBuilder();
        sb.append(address.getHostAddress());
        sb.append(IP_PORT_SPLITTER);
        sb.append(port);
        return sb.toString();
    }

    private ClientSettings() {
    }
}
