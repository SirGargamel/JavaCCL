package cz.tul.comm.persistence;

import cz.tul.comm.Constants;
import cz.tul.comm.client.IServerInterface;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.gui.UserLogging;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
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
public final class ClientSettings {

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
        boolean result = false;

        InetAddress ip = null;
        int port = Constants.DEFAULT_PORT;
        try {
            File s = new File(SERIALIZATION_NAME);
            List<Field> fields = SimpleXMLFile.loadSimpleXMLFile(s);
            for (Field f : fields) {
                switch (f.getName()) {
                    case FIELD_NAME_SERVER:
                        try {
                            String[] split = f.getValue().split(IP_PORT_SPLITTER);
                            ip = InetAddress.getByName(split[0]);
                            port = Integer.valueOf(split[1]);
                        } catch (UnknownHostException ex) {
                            UserLogging.showWarningToUser("Unknown server ip found in settings - " + ex.getLocalizedMessage());
                            log.log(Level.WARNING, "Unkonwn server ip found in settings", ex);
                        } catch (NumberFormatException ex) {
                            UserLogging.showWarningToUser("Unknown server port found in settings - " + ex.getLocalizedMessage());
                            log.log(Level.WARNING, "Unkonwn server port found in settings", ex);
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            UserLogging.showWarningToUser("Illegal parameters for server IP and port found in settings - " + f.getValue());
                            log.log(Level.WARNING, "Unkonwn server IP and port found in settings.", ex);
                        }
                        break;
                    default:
                        log.log(Level.CONFIG, "Unknown field - {0}", f.getName());
                        break;
                }
            }

            if (ip != null) {
                reg.registerServer(ip, port);
            }

            result = true;
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error accessing server settings.", ex);
            UserLogging.showErrorToUser("Cannot access " + SERIALIZATION_NAME);
        } catch (SAXException ex) {
            log.log(Level.WARNING, "XML parsing error.", ex);
            UserLogging.showErrorToUser("Server settings file is in wrong format, check logs for details.");
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

        xml.addField(new Field(
                FIELD_NAME_SERVER,
                composeServerAddress(serverCommunicator.getAddress(), serverCommunicator.getPort())));

        boolean result = false;
        try {
            result = xml.storeXML(new File(SERIALIZATION_NAME));
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error accessing server settings.", ex);
            UserLogging.showErrorToUser("Cannot access " + SERIALIZATION_NAME);
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
}
