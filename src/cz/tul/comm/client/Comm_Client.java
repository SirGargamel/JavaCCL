package cz.tul.comm.client;

import cz.tul.comm.ComponentSwitches;
import cz.tul.comm.Constants;
import cz.tul.comm.IService;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.communicator.Status;
import cz.tul.comm.history.History;
import cz.tul.comm.history.IHistoryManager;
import cz.tul.comm.history.sorting.DefaultSorter;
import cz.tul.comm.messaging.BasicConversator;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.messaging.MessageHeaders;
import cz.tul.comm.messaging.job.IAssignmentListener;
import cz.tul.comm.persistence.ClientSettings;
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
public class Comm_Client implements IService, IServerInterface, Client {

    private static final Logger log = Logger.getLogger(Comm_Client.class.getName());
    /**
     * Default port on which will client listen.
     */
    private static final int TIMEOUT = 1_000;

    /**
     * Create and initialize new instance of client.
     *
     * @param port
     * @return new Client instance
     */
    public static Client initNewClient(final int port) {
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
    public static Client initNewClient() {
        int port = Constants.DEFAULT_PORT;
        Client c = initNewClient(port);
        while (c == null) {
            c = initNewClient(++port);
            if (c != null) {
                // perhaps client running on same machine as server
                c.registerToServer(InetAddress.getLoopbackAddress(), Constants.DEFAULT_PORT);
            }
        }
        return c;
    }
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

        if (ComponentSwitches.useClientDiscovery) {
            try {
                sdd = new ServerDiscoveryDaemon(this);
            } catch (SocketException ex) {
                log.log(Level.INFO, "Failed to initiate server discovery daemon, server must be configured manually.");
                log.log(Level.FINE, "Failed to initiate server discovery daemon.", ex);
            }
        }

        if (ComponentSwitches.useSettings) {
            if (!ClientSettings.deserialize(this)) {
                log.warning("Error loading client settings.");
            }
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    ClientSettings.serialize(comm);
                }
            }));
        }

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                stopService();
            }
        }));
    }

    @Override
    public void registerToServer(final InetAddress address, final int port) {
        log.log(Level.CONFIG, "Registering new server IP and port - {0}:{1}", new Object[]{address.getHostAddress(), port});        
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
     * @return true for successfull data sending
     */
    @Override
    public boolean sendData(final Object data) {
        log.log(Level.INFO, "Sending data to server - {0}", data.toString());
        if (comm == null) {
            throw new NullPointerException("No server communicator set");
        } else {
            if (!isServerUp()) {
                log.warning("Server could not be contacted.");
                return false;
            } else {
                return comm.sendData(data);
            }
        }
    }

    /**
     * Export history as is to XML file.
     *
     * @return true for successfull export.
     */
    @Override
    public boolean exportHistory() {
        log.info("Exporting histry to default location with no sorting.");
        return history.export(new File(""), new DefaultSorter());
    }

    /**
     * @return history manager for this client
     */
    @Override
    public IHistoryManager getHistory() {
        return history;
    }

    /**
     * @return interface for listener registration
     */
    @Override
    public IListenerRegistrator getListenerRegistrator() {
        return serverSocket;
    }

    /**
     * Atach interface, that will handle assignment computation.
     *
     * @param assignmentListener class hnadling assignment computation
     */
    @Override
    public void assignAssignmentListener(IAssignmentListener assignmentListener) {
        this.assignmentListener = assignmentListener;
    }

    IAssignmentListener getAssignmentListener() {
        return assignmentListener;
    }

    @Override
    public Communicator getServerComm() {
        return comm;
    }

    /**
     * @return client status
     */
    @Override
    public Status getStatus() {
        return status;
    }

    private void start() {
        if (sdd != null) {
            sdd.start();
        } else if (ComponentSwitches.useClientAutoConnectLocalhost) {
            log.info("Could not init server discovery, tryiing to connect to local host.");
            registerToServer(InetAddress.getLoopbackAddress(), Constants.DEFAULT_PORT);
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
}
