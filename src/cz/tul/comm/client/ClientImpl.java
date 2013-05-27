package cz.tul.comm.client;

import cz.tul.comm.socket.ClientLister;
import cz.tul.comm.ComponentSwitches;
import cz.tul.comm.Constants;
import cz.tul.comm.IService;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.communicator.CommunicatorImpl;
import cz.tul.comm.communicator.CommunicatorInner;
import cz.tul.comm.communicator.Status;
import cz.tul.comm.history.History;
import cz.tul.comm.history.HistoryManager;
import cz.tul.comm.history.sorting.DefaultSorter;
import cz.tul.comm.job.client.AssignmentListener;
import cz.tul.comm.job.client.ClientJobManagerImpl;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.messaging.MessageHeaders;
import cz.tul.comm.persistence.ClientSettings;
import cz.tul.comm.socket.IDFilter;
import cz.tul.comm.socket.ListenerRegistrator;
import cz.tul.comm.socket.ServerSocket;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class enclosing client to server communication. Allows data sending and has a
 * registry for message handling.
 *
 * @author Petr Ječmen
 */
public class ClientImpl implements IService, ServerInterface, Client, IDFilter, ClientLister {

    private static final Logger log = Logger.getLogger(ClientImpl.class.getName());    

    /**
     * Create and initialize new instance of client at given port.
     *
     * @param port target listening port
     * @return new Client instance
     */
    public static Client initNewClient(final int port) {
        ClientImpl result;
        try {
            result = new ClientImpl();
            result.start(port);
            log.log(Level.INFO, "New client created on port {0}", port);
        } catch (IOException ex) {
            log.log(Level.WARNING, "Failed to initialize client on port " + port, ex);
            result = null;
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
        }
        return c;
    }
    private ServerSocket serverSocket;
    private final HistoryManager history;
    private CommunicatorInner comm;
    private SystemMessageHandler csm;
    private ClientJobManagerImpl jm;
    private ServerDiscoveryDaemon sdd;

    private ClientImpl() {
        history = new History();

        if (ComponentSwitches.useClientDiscovery) {
            try {
                sdd = new ServerDiscoveryDaemon(this);
            } catch (SocketException ex) {
                log.log(Level.FINE, "Failed to initiate server discovery daemon.", ex);
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                sendDataToServer(new Message(Constants.ID_SYS_MSG, MessageHeaders.LOGOUT, comm.getId()));                
                stopService();
            }
        }));
    }

    @Override
    public boolean registerToServer(final InetAddress address, final int port) {
        log.log(Level.CONFIG, "Registering new server IP and port - {0}:{1}", new Object[]{address.getHostAddress(), port});
        boolean result = false;
        comm = CommunicatorImpl.initNewCommunicator(address, port);
        if (isServerUp()) {
            final Message login = new Message(Constants.ID_SYS_MSG, MessageHeaders.LOGIN, serverSocket.getPort());
            final Object id = comm.sendData(login);

            if (id instanceof UUID) {
                comm.setId((UUID) id);
                result = true;
                log.log(Level.INFO, "Client has been registered to new server, new ID has been received - {0}", comm.getId());
            } else {
                log.log(Level.WARNING, "Invalid response received - {0}", id.toString());
            }
        } else {
            log.log(Level.CONFIG, "Server could not be contacted.");
        }
        return result;
    }

    @Override
    public void deregisterFromServer() {
        final Message m = new Message(MessageHeaders.LOGOUT, comm.getId());
        sendDataToServer(m);
        comm = null;
    }

    @Override
    public boolean isServerUp() {
        boolean serverStatus = false;
        if (comm != null) {
            Status s = comm.getStatus();
            if (s.equals(Status.ONLINE) || s.equals(Status.PASSIVE)) {
                serverStatus = true;
            }
        }
        log.log(Level.INFO, "Is server running - {0}", serverStatus);
        return serverStatus;
    }

    @Override
    public Object sendDataToServer(final Object data) {
        log.log(Level.INFO, "Sending data to server - {0}", data.toString());
        if (comm == null) {
            throw new NullPointerException("No server communicator set.");
        } else {
            if (!isServerUp()) {
                log.warning("Server could not be contacted.");
                return false;
            } else {
                return comm.sendData(data);
            }
        }
    }

    @Override
    public boolean exportHistory() {
        log.info("Exporting histry to default location with no sorting.");
        return history.export(new File(""), new DefaultSorter());
    }

    @Override
    public HistoryManager getHistory() {
        return history;
    }

    @Override
    public ListenerRegistrator getListenerRegistrator() {
        return serverSocket;
    }

    @Override
    public void setAssignmentListener(AssignmentListener assignmentListener) {
        jm.setAssignmentListener(assignmentListener);
    }

    @Override
    public Communicator getServerComm() {
        return comm;
    }

    private void start(final int port) throws IOException {
        serverSocket = ServerSocket.createServerSocket(port, this, this);
        serverSocket.registerHistory(history);

        csm = new SystemMessageHandler();
        getListenerRegistrator().setIdListener(Constants.ID_SYS_MSG, csm, true);

        jm = new ClientJobManagerImpl(this);
        getListenerRegistrator().setIdListener(Constants.ID_JOB_MANAGER, jm, true);

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
        if (sdd != null) {
            sdd.start();
        } else if (ComponentSwitches.useClientAutoConnectLocalhost && !isServerUp()) {
            log.info("Could not init server discovery, trying to connect to local host.");
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

    @Override
    public boolean isIdAllowed(UUID id) {
        final UUID commId = comm.getId();
        if (commId == null) {
            return true;
        } else {
            return comm.getId().equals(id);
        }
    }

    @Override
    public void requestAssignment() {
        jm.requestAssignment();
    }

    @Override
    public int getLocalSocketPort() {
        return serverSocket.getPort();
    }

    @Override
    public Collection<Communicator> getClients() {
        final Collection<Communicator> result = new ArrayList<>(1);
        result.add(comm);
        return result;
    }

    @Override
    public Object sendDataToClient(final UUID clientId, final Object data, final int timeout) throws UnknownHostException, IllegalArgumentException {
        // ask server for IP and port
        final Message serverQuestion = new Message(Constants.ID_SYS_MSG, MessageHeaders.CLIENT_IP_PORT_QUESTION, clientId);
        final Object clientIpPort = sendDataToServer(serverQuestion);
        if (clientIpPort instanceof String) {
            final String ipPort = (String) clientIpPort;
            String[] split = ipPort.split(Constants.DELIMITER);
            final InetAddress ip = InetAddress.getByName(split[0]);
            final int port = Integer.valueOf(split[1]);
            // create Comm to client
            final Communicator c = CommunicatorImpl.initNewCommunicator(ip, port);
            // send data to client and return result        
            return c.sendData(data, timeout);
        } else {
            throw new IllegalArgumentException("Illegal client id used - " + clientId.toString());
        }
    }

    @Override
    public Object sendDataToClient(UUID clientId, Object data) throws UnknownHostException, IllegalArgumentException {
        return sendDataToClient(clientId, data, 0);
    }
}
