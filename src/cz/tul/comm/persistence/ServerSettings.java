package cz.tul.comm.persistence;

import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.gui.UserLogging;
import cz.tul.comm.server.IClientManager;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xml.sax.SAXException;

/**
 * Settings for server part.
 *
 * @author Petr Ječmen
 */
public final class ServerSettings implements Serializable {

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
    public static boolean deserialize(final IClientManager clientManager) {
        log.log(Level.CONFIG, "Deserializing server settings.");
        boolean result = false;

        try {
            File s = new File(SERIALIZATION_NAME);
            List<Field> fields = SimpleXMLFile.loadSimpleXMLFile(s);
            for (Field f : fields) {
                switch (f.getName()) {
                    case FIELD_NAME_CLIENT:
                        try {
                            String[] split = f.getValue().split(IP_PORT_SPLITTER);
                            clientManager.registerClient(InetAddress.getByName(split[0]), Integer.valueOf(split[1]));
                        } catch (UnknownHostException | NumberFormatException | ArrayIndexOutOfBoundsException ex) {
                            UserLogging.showWarningToUser("Unknown host found in settings - " + ex.getLocalizedMessage());
                            log.log(Level.WARNING, "Unkonwn host found in settings", ex);
                        }
                        break;
                    default:
                        log.log(Level.CONFIG, "Unknown field - {0}", f.getName());
                        break;
                }
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
     * Store server settings to disk.
     *
     * @param clientManager interface for managing clients
     * @return true for successfull save
     */
    public static boolean serialize(final IClientManager clientManager) {
        log.log(Level.CONFIG, "Serializing server settings.");
        SimpleXMLFile xml = new SimpleXMLFile();

        Set<Communicator> comms = clientManager.getClients();
        StringBuilder sb = new StringBuilder();
        for (Communicator c : comms) {
            sb.append(c.getAddress().getHostAddress());
            sb.append(IP_PORT_SPLITTER);
            sb.append(c.getPort());
            xml.addField(new Field(FIELD_NAME_CLIENT, sb.toString()));
            sb.setLength(0);
        }

        boolean result = false;
        try {
            result = xml.storeXML(new File(SERIALIZATION_NAME));
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error accessing server settings.", ex);
            UserLogging.showErrorToUser("Cannot access " + SERIALIZATION_NAME);
        }

        return result;
    }
}
