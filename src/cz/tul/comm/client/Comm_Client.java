package cz.tul.comm.client;

import cz.tul.comm.persistence.ClientSettings;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.IService;
import cz.tul.comm.communicator.Status;
import cz.tul.comm.gui.UserLogging;
import cz.tul.comm.history.History;
import cz.tul.comm.history.IHistoryManager;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.messaging.MessageHeaders;
import cz.tul.comm.server.Comm_Server;
import cz.tul.comm.socket.IListenerRegistrator;
import cz.tul.comm.socket.ServerSocket;
import java.io.File;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class enclosing client to server communication. Allows data sending and has a
 * registry for message handling.
 *
 * @author Petr Ječmen
 */
public final class Comm_Client implements IService, IServerRegistrator {

    private static final Logger log = Logger.getLogger(Comm_Client.class.getName());
    /**
     * Default port on which will client listen.
     */
    public static final int PORT = 5253;
    private static final int TIMEOUT = 1000;
    private final ServerSocket serverSocket;
    private final IHistoryManager history;
    private Communicator comm;
    private Status status;
    private final ClientSystemMessaging csm;
    private ServerDiscoveryDaemon sdd;

    private Comm_Client(final int port) {
        history = new History();

        serverSocket = ServerSocket.createServerSocket(port);
        serverSocket.registerHistory(history);
        registerServer(InetAddress.getLoopbackAddress(), Comm_Server.PORT);

        status = Status.ONLINE;
        csm = new ClientSystemMessaging(this);
        getListenerRegistrator().addIpListener(comm.getAddress(), csm, true);
        try {
            sdd = new ServerDiscoveryDaemon(this);
        } catch (SocketException ex) {
            log.log(Level.WARNING, "Failed to initiate server discovery daemon.", ex);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                ClientSettings.serialize(comm);
            }
        }));
    }

    @Override
    public void registerServer(final InetAddress address, final int port) {
        try {
            comm = new Communicator(address, port);
            if (!isServerUp()) {
                sdd.setServerUp(false);
            }
        } catch (IllegalArgumentException ex) {
            UserLogging.showErrorToUser(ex.getLocalizedMessage());
            log.log(Level.WARNING, "Illegal parameters for Communicator.", ex);
        }
    }

    @Override
    public boolean isServerUp() {
        return comm.sendData(new Message(MessageHeaders.KEEP_ALIVE, null), TIMEOUT);
    }

    /**
     *
     * @param address new IP of server
     */
    public void setServerAdress(final InetAddress address, final int port) {
        getListenerRegistrator().removeIpListener(null, csm);
        getListenerRegistrator().addIpListener(address, csm, true);
        registerServer(address, port);
    }

    /**
     * Send data to server.
     *
     * @param data data for sending
     */
    public void sendData(final Object data) {
        if (comm == null || !isServerUp()) {
            UserLogging.showWarningToUser("Server could not be contacted, please recheck the settings");
            sdd.setServerUp(false);
        } else {
            comm.sendData(data);
        }
    }

    /**
     * Export history as is to XML file.
     *
     * @param target target file
     * @return true for successfull export.
     */
    public boolean exportHistory() {
        return history.export(new File(""), null);
    }

    /**
     * @return history manager for this client
     */
    public IHistoryManager getHistory() {
        return history;
    }

    /**
     * @return interface for listener registration
     */
    public IListenerRegistrator getListenerRegistrator() {
        return serverSocket;
    }

    public Status getStatus() {
        return status;
    }

    /**
     * Create and initialize new instance of client.
     *
     * @return new Client instance
     */
    public static Comm_Client initNewClient(final int port) {
        final Comm_Client result = new Comm_Client(port);

        result.start();

        return result;
    }

    public static Comm_Client initNewClient() {
        return initNewClient(PORT);
    }

    private void start() {
        // load settings
        if (!ClientSettings.deserialize(this)) {
            // TODO tell user that settings are wrong
        }
        // registration
        final Message m = new Message(MessageHeaders.LOGIN, serverSocket.getPort());
        if (!comm.sendData(m)) {
            log.warning("Registering client to server failed.");
        }
        if (sdd != null) {
            sdd.start();
        }
    }

    @Override
    public void stopService() {
        serverSocket.stopService();
        sdd.stopService();
    }
}
