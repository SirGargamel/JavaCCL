package cz.tul.comm.persistence;

import cz.tul.comm.Constants;
import cz.tul.comm.client.IServerInterface;
import cz.tul.comm.communicator.Communicator;
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
    private static final String SERIALIZATION_NAME = "clientSettings.xml";
    private static final String IP_PORT_SPLITTER = ":";
    private static final String FIELD_NAME_SERVER = "serverIp";

    /**
     * Save client settings to disk.
     *
     * @param reg server parameters registrator
     * @return true for successfull deserialization
     */
    public static boolean deserialize(final IServerInterface reg) {
        log.log(Level.CONFIG, "Deserializing client settings.");
        boolean result = true;

        File s = new File(SERIALIZATION_NAME);
        if (s.exists()) {
            InetAddress ip = null;
            int port = Constants.DEFAULT_PORT;
            try {
                Map<String, String> fields = SimpleXMLFile.loadSimpleXMLFile(s);
                for (String f : fields.keySet()) {
                    switch (f) {
                        case FIELD_NAME_SERVER:
                            try {
                                String[] split = fields.get(f).split(IP_PORT_SPLITTER);
                                ip = InetAddress.getByName(split[0]);
                                port = Integer.valueOf(split[1]);
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
                    reg.registerToServer(ip, port);
                }
            } catch (IOException ex) {
                result = false;
                log.log(Level.WARNING, "Error accessing client settings at " + SERIALIZATION_NAME + ".", ex);
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
     * Load client settings from disk.
     *
     * @param serverCommunicator server communicator
     * @return true for successfull save
     */
    public static boolean serialize(final Communicator serverCommunicator) {
        log.log(Level.CONFIG, "Serializing client settings.");

//        final ClientSettings s = new ClientSettings();
//
//        s.serverAdress = composeServerAddress(serverCommunicator.getAddress(), serverCommunicator.getPort());
//
//        return SerializationUtils.saveItemToDiscAsXML(new File(SERIALIZATION_NAME), s);

        SimpleXMLFile xml = new SimpleXMLFile();

        xml.addField(
                FIELD_NAME_SERVER,
                composeServerAddress(serverCommunicator.getAddress(), serverCommunicator.getPort()));

        boolean result = false;
        try {
            result = xml.storeXML(new File(SERIALIZATION_NAME));
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error accessing client settings at " + SERIALIZATION_NAME + ".", ex);
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
