package cz.tul.comm.client;

import cz.tul.comm.Constants;
import cz.tul.comm.persistence.ClientSettings;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.IService;
import cz.tul.comm.communicator.Status;
import cz.tul.comm.gui.UserLogging;
import cz.tul.comm.history.History;
import cz.tul.comm.history.IHistoryManager;
import cz.tul.comm.history.sorting.DefaultSorter;
import cz.tul.comm.messaging.BasicConversator;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.messaging.MessageHeaders;
import cz.tul.comm.messaging.job.IAssignmentListener;
import cz.tul.comm.socket.IListenerRegistrator;
import cz.tul.comm.socket.ServerSocket;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class enclosing client to server communication. Allows data sending and has a
 * registry for message handling.
 *
 * @author Petr Ječmen
 */
public final class Comm_Client implements IService, IServerInterface {

    private static final Logger log = Logger.getLogger(Comm_Client.class.getName());
    /**
     * Default port on which will client listen.
     */
    public static final int PORT = 5253;
    private static final int TIMEOUT = 1000;
    private final ServerSocket serverSocket;
    private final IHistoryManager history;
    private IAssignmentListener assignmentListener;
    private Communicator comm;
    private Status status;
    private final ClientSystemMessaging csm;
    private ServerDiscoveryDaemon sdd;

    private Comm_Client(final int port) throws IOException {
        history = new History();

        serverSocket = ServerSocket.createServerSocket(port);
        serverSocket.registerHistory(history);

        status = Status.ONLINE;
        csm = new ClientSystemMessaging(this);
        serverSocket.addMessageObserver(csm);

        try {
            sdd = new ServerDiscoveryDaemon(this);
        } catch (SocketException ex) {
            log.log(Level.FINE, "Failed to initiate server discovery daemon.", ex);
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
        log.log(Level.CONFIG, "Registering new server IP and port - {0}:{1}", new Object[]{address.getHostAddress(), port});
        try {
            comm = Communicator.initNewCommunicator(address, port);
            if (isServerUp()) {
                final Message login = new Message(MessageHeaders.LOGIN, serverSocket.getPort());
                BasicConversator bs = new BasicConversator(comm, serverSocket);

                final Object id = bs.sendAndReceiveData(login);
                if (id instanceof Message) {
                    final Message m = (Message) id;
                    if (m.getHeader().equals(MessageHeaders.LOGIN)
                            && m.getData() instanceof UUID) {
                        comm.setId((UUID) m.getData());
                        log.log(Level.INFO, "Client has been registered to new server, new ID has been received - {0}", comm.getId());
                    } else {
                        log.log(Level.WARNING, "Invalid response received - {0}", m.toString());
                    }
                } else {
                    log.log(Level.WARNING, "Invalid response received - {0}", id.toString());
                }
            } else {
                log.log(Level.CONFIG, "Server could not be contacted");
            }
        } catch (IllegalArgumentException ex) {
            UserLogging.showErrorToUser(ex.getLocalizedMessage());
            log.log(Level.WARNING, "Illegal parameters for Communicator.", ex);
        }
    }
    
    @Override
    public void deregisterFromServer() {
        final Message m = new Message(MessageHeaders.LOGOUT, comm.getId());
        sendData(m);
        comm = null;
    }

    @Override
    public boolean isServerUp() {
        boolean serverStatus;
        if (comm != null) {
            serverStatus = comm.sendData(new Message(MessageHeaders.KEEP_ALIVE, null), TIMEOUT);
        } else {
            serverStatus = false;
        }
        log.log(Level.INFO, "Is server running - {0}", serverStatus);
        return serverStatus;
    }

    /**
     * Send data to server.
     *
     * @param data data for sending
     */
    public boolean sendData(final Object data) {
        log.log(Level.INFO, "Sending data to server - {0}", data.toString());
        if (comm == null || !isServerUp()) {
            UserLogging.showWarningToUser("Server could not be contacted, please recheck the settings");
            return false;
        } else {
            return comm.sendData(data);
        }
    }

    /**
     * Export history as is to XML file.
     *
     * @return true for successfull export.
     */
    public boolean exportHistory() {
        log.info("Exporting histry to default location with no sorting.");
        return history.export(new File(""), new DefaultSorter());
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

    public void assignAssignmentListener(IAssignmentListener assignmentListener) {
        this.assignmentListener = assignmentListener;
    }

    IAssignmentListener getAssignmentListener() {
        return assignmentListener;
    }
    
    Communicator getServerCommunicator() {
        return comm;
    }

    /**
     * @return client status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Create and initialize new instance of client.
     *
     * @param port
     * @return new Client instance
     */
    public static Comm_Client initNewClient(final int port) {
        Comm_Client result = null;
        try {
            result = new Comm_Client(port);
            result.start();
            log.log(Level.INFO, "New client created on port {0}", port);
        } catch (IOException ex) {
            log.log(Level.WARNING, "Failed to initialize client on port " + port, ex);
        }

        return result;
    }

    /**
     *
     * @return new client instance on default port
     */
    public static Comm_Client initNewClient() {
        int port = Constants.DEFAULT_PORT;
        Comm_Client c = initNewClient(port);
        while (c == null) {
            c = initNewClient(++port);
            if (c != null) {
                // perhaps client running on same machine as server
                c.registerServer(InetAddress.getLoopbackAddress(), Constants.DEFAULT_PORT);
            }
        }
        return c;
    }

    private void start() {
        // load settings
        if (!ClientSettings.deserialize(this)) {
            // TODO tell user that settings are wrong
        }
        if (sdd != null) {
            sdd.start();
        }
    }

    @Override
    public void stopService() {
        serverSocket.stopService();
        if (sdd != null) {
            sdd.stopService();
        }
        log.fine("Client has been stopped.");
    }

    @Override
    public int getServerPort() {
        return serverSocket.getPort();
    }    
}
