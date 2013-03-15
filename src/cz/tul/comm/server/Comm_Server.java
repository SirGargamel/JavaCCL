package cz.tul.comm.server;

import cz.tul.comm.IService;
import cz.tul.comm.SerializationUtils;
import cz.tul.comm.gui.UserLogging;
import cz.tul.comm.history.History;
import cz.tul.comm.history.IHistoryManager;
import cz.tul.comm.socket.Communicator;
import cz.tul.comm.socket.IListenerRegistrator;
import cz.tul.comm.socket.ServerSocket;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class enclosing server-client communication. Handles custom data sending to
 * given client, whole job computation and client status monitoring.
 *
 * @author Petr Ječmen
 */
public final class Comm_Server implements IService {

    private static final Logger log = Logger.getLogger(Comm_Server.class.getName());
    /**
     * default port on which server will listen
     */
    public static final int PORT = 5252;
    private final Settings settings;
    private final ClientDB clients;
    private final ServerSocket serverSocket;
    private final IHistoryManager history;

    private Comm_Server() {
        // client DB
        clients = new ClientDB();
        File s = new File(Settings.SERIALIZATION_NAME);
        if (s.exists()) {
            Object in = SerializationUtils.loadXMLItemFromDisc(s);
            if (in instanceof Settings) {
                settings = (Settings) in;
                for (String a : settings.getClients()) {
                    try {
                        registerClient(InetAddress.getByName(a));
                    } catch (UnknownHostException ex) {
                        UserLogging.showWarningToUser("Unknown host found in settings - " + ex.getLocalizedMessage());
                        log.log(Level.WARNING, "Unkonwn host found in settings", ex);
                    }
                }
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

        serverSocket = ServerSocket.createServerSocket(PORT);

        history = new History();
    }

    private void saveData() {
        final Set<String> addresses = settings.getClients();
        addresses.clear();
        for (Communicator c : clients.getClients()) {
            addresses.add(c.getAddress().getHostAddress());
        }

        SerializationUtils.saveItemToDiscAsXML(new File(Settings.SERIALIZATION_NAME), settings);
    }

    /**
     * Register new client communicationg on given IP and on default port.
     *
     * @param adress client IP
     * @return
     */
    public Communicator registerClient(final InetAddress adress) {
        return clients.registerClient(adress, settings.getDefaultClientPort());
    }
    
    /**
     * Export history as is to XML file.
     *
     * @param target target file
     * @return true for successfull export.
     */
    public boolean exportHistory(final File target) {
        return history.export(target, null);
    }

    /**
     * @return history manager for this client
     */
    public IHistoryManager getHistory() {
        return history;
    }

    public IClientManager getClientManager() {
        return clients;
    }

    /**
     * Interface for registering new data listeners.
     *
     * @return
     */
    public IListenerRegistrator getListenerRegistrator() {
        return serverSocket;
    }

    /**
     * Create and initialize new instance of server.
     *
     * @return new instance of Comm_Server
     */
    public static Comm_Server initNewServer() {
        final Comm_Server result = new Comm_Server();

        result.start();

        return result;
    }

    void start() {
    }

    @Override
    public void stopService() {
        serverSocket.stopService();
    }
}
