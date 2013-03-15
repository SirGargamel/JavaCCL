package cz.tul.comm.client;

import cz.tul.comm.socket.Communicator;
import cz.tul.comm.IService;
import cz.tul.comm.SerializationUtils;
import cz.tul.comm.gui.UserLogging;
import cz.tul.comm.history.History;
import cz.tul.comm.history.IHistoryManager;
import cz.tul.comm.history.sorting.HistorySorter;
import cz.tul.comm.socket.IListenerRegistrator;
import cz.tul.comm.socket.ServerSocket;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class enclosing client to server communication. Allows data sending and has a
 * registry for message handling.
 *
 * @author Petr Ječmen
 */
public final class Comm_Client implements IService {

    private static final Logger log = Logger.getLogger(Comm_Client.class.getName());
    /**
     * Default port on which will client listen.
     */
    public static final int PORT = 5253;
    private final ServerSocket serverSocket;
    private Communicator comm;
    private final Settings settings;
    private final IHistoryManager history;

    private Comm_Client() {
        serverSocket = ServerSocket.createServerSocket(PORT);

        File s = new File(Settings.SERIALIZATION_NAME);
        if (s.exists()) {
            Object in = SerializationUtils.loadXMLItemFromDisc(s);
            if (in instanceof Settings) {
                settings = (Settings) in;
            } else {
                settings = new Settings();
            }
        } else {
            settings = new Settings();
        }
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                saveData();
            }
        }));

        prepareServerCommunicator();
        
        history = new History();
    }

    private void prepareServerCommunicator() {
        try {
            InetAddress serverIp = InetAddress.getByName(settings.getServerAdress());
            comm = new Communicator(serverIp, settings.getServerPort());
        } catch (UnknownHostException ex) {
            UserLogging.showWarningToUser("Unknown host set in settings - " + settings.getServerAdress());
            log.log(Level.WARNING, "Unkonwn host set in settings.", ex);
        } catch (IllegalArgumentException ex) {
            UserLogging.showErrorToUser(ex.getLocalizedMessage());
            log.log(Level.WARNING, "Illegal parameters for Communicator.", ex);
        }
    }

    /**
     *
     * @param address new IP of server
     */
    public void setServerAdress(final InetAddress address) {
        settings.setServerAdress(address.getHostAddress());
        prepareServerCommunicator();
    }

    /**
     *
     * @param port new server port
     */
    public void setServerPort(final int port) {
        settings.setServerPort(port);
        prepareServerCommunicator();
    }

    /**
     * Send data to server.
     *
     * @param data data for sending
     */
    public void sendData(final Object data) {
        if (comm != null) {
            comm.sendData(data);
        }
    }
    
    /**
     * @return history manager for this client
     */
    public IHistoryManager getHistory() {
        return history;
    }

    void saveData() {
        SerializationUtils.saveItemToDiscAsXML(new File(Settings.SERIALIZATION_NAME), settings);
    }

    /**
     * @return interface for listener registration
     */
    public IListenerRegistrator getListenerRegistrator() {
        return serverSocket;
    }

    /**
     * Create and initialize new instance of client.
     *
     * @return new Client instance
     */
    public static Comm_Client initNewClient() {
        final Comm_Client result = new Comm_Client();

        result.start();

        return result;
    }

    private void start() {
    }

    @Override
    public void stopService() {
        serverSocket.stopService();
    }
}
