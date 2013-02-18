package cz.tul.comm.server;

import cz.tul.comm.IService;
import cz.tul.comm.SerializationUtils;
import cz.tul.comm.gui.UserLogging;
import cz.tul.comm.socket.Communicator;
import cz.tul.comm.socket.ServerSocket;
import java.io.File;
import java.net.BindException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class Comm_Server implements IService {

    private static final Logger log = Logger.getLogger(Comm_Server.class.getName());
    public static final int PORT = 5252;
    private final Settings settings;
    private final ClientDB clients;
    private final ServerSocket serverSocket;
    private final MessageHandler msgHandler;

    private Comm_Server() {
        msgHandler = new MessageHandler();

        // client DB
        clients = new ClientDB();
        File s = new File(Settings.SERIALIZATION_NAME);
        if (s.exists()) {
            Object in = SerializationUtils.loadXMLItemFromDisc(s);
            if (in instanceof Settings) {
                settings = (Settings) in;
                for (String a : settings.getClients()) {
                    try {
                        clients.registerClient(InetAddress.getByName(a));
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

        ServerSocket tmp = null;
        try {
            tmp = new ServerSocket(PORT);
        } catch (BindException ex) {
            UserLogging.showErrorToUser("Error creating server socket on port " + PORT);
        }

        serverSocket = tmp;
        serverSocket.addMessageHandler(msgHandler);
    }

    public Communicator registerClient(final InetAddress adress) {
        Communicator result = clients.registerClient(adress);
        result.registerMessageHandler(msgHandler);
        return result;
    }

    public void deregisterClient(final InetAddress adress) {
        clients.deregisterClient(adress);
    }

    public Communicator getClient(final InetAddress adress) {
        return clients.getClient(adress);
    }

    public Set<Communicator> getClients() {
        return clients.getClients();
    }

    public void saveData() {
        final Set<String> addresses = settings.getClients();
        addresses.clear();
        for (Communicator c : clients.getClients()) {
            addresses.add(c.getAddress().getHostAddress());
        }

        SerializationUtils.saveItemToDiscAsXML(new File(Settings.SERIALIZATION_NAME), settings);
    }

    public static Comm_Server initNewServer() {
        final Comm_Server result = new Comm_Server();

        result.start();

        return result;
    }

    void start() {
        serverSocket.start();
    }

    @Override
    public void stopService() {
        serverSocket.stopService();
    }
}
