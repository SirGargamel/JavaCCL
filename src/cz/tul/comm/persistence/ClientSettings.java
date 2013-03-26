package cz.tul.comm.persistence;

import cz.tul.comm.client.IServerRegistrator;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.gui.UserLogging;
import cz.tul.comm.server.Comm_Server;
import java.io.File;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Settings for client part. Default settings for server address is loopback
 * adress (eg. server and client are run on same machine) on default server
 * port.
 *
 * @author Petr Ječmen
 */
public final class ClientSettings implements Serializable {

    private static final Logger log = Logger.getLogger(ClientSettings.class.getName());
    private static final String SERIALIZATION_NAME = "clientSettings.xml";
    private static final long serialVersionUID = 3L;
    private static final String IP_PORT_SPLITTER = ":";
    private static final InetAddress DEFAULT_SERVER_IP = InetAddress.getLoopbackAddress();
    private String serverAdress;

    /**
     * Initialize storge.
     */
    public ClientSettings() {
        serverAdress = composeServerAddress(DEFAULT_SERVER_IP, Comm_Server.PORT);
    }

    /**
     *
     * @return
     */
    public String getServerAdress() {
        return serverAdress;
    }

    /**
     *
     * @param serverAdress
     */
    public void setServerAdress(String serverAdress) {
        this.serverAdress = serverAdress;
    }

    /**
     * Save client settings to disk.
     *
     * @param reg server parameters registrator
     * @return true for successfull deserialization
     */
    public static boolean deserialize(final IServerRegistrator reg) {
        log.log(Level.FINER, "Deserializing client settings.");

        boolean result = true;
        File s = new File(SERIALIZATION_NAME);

        InetAddress ip = null;
        int port = Comm_Server.PORT;
        if (s.exists()) {
            Object in = SerializationUtils.loadXMLItemFromDisc(s);
            if (in instanceof ClientSettings) {
                ClientSettings settings = (ClientSettings) in;
                try {
                    String[] split = settings.serverAdress.split(IP_PORT_SPLITTER);
                    ip = InetAddress.getByName(split[0]);
                    port = Integer.valueOf(split[1]);
                } catch (UnknownHostException ex) {
                    UserLogging.showWarningToUser("Unknown server ip found in settings - " + ex.getLocalizedMessage());
                    log.log(Level.WARNING, "Unkonwn server ip found in settings", ex);
                } catch (NumberFormatException ex) {
                    UserLogging.showWarningToUser("Unknown server port found in settings - " + ex.getLocalizedMessage());
                    log.log(Level.WARNING, "Unkonwn server port found in settings", ex);
                } catch (ArrayIndexOutOfBoundsException ex) {
                    UserLogging.showWarningToUser("Illegal parameters for server IP and port found in settings - " + settings.serverAdress);
                    log.log(Level.WARNING, "Unkonwn server IP and port found in settings.", ex);
                }
            } else {
                result = false;
            }
        } else {
            result = false;
        }

        if (ip != null) {
            reg.registerServer(ip, port);
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
        log.log(Level.FINER, "Serializing client settings.");

        final ClientSettings s = new ClientSettings();

        s.serverAdress = composeServerAddress(serverCommunicator.getAddress(), serverCommunicator.getPort());

        return SerializationUtils.saveItemToDiscAsXML(new File(SERIALIZATION_NAME), s);
    }

    private static String composeServerAddress(final InetAddress address, final int port) {
        StringBuilder sb = new StringBuilder();
        sb.append(address.getHostAddress());
        sb.append(IP_PORT_SPLITTER);
        sb.append(port);
        return sb.toString();
    }
}
