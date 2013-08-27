package cz.tul.comm.client;

import cz.tul.comm.socket.ClientLister;
import cz.tul.comm.ComponentSwitches;
import cz.tul.comm.Constants;
import cz.tul.comm.GenericResponses;
import cz.tul.comm.IService;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.communicator.CommunicatorImpl;
import cz.tul.comm.communicator.CommunicatorInner;
import cz.tul.comm.communicator.Status;
import cz.tul.comm.exceptions.ConnectionException;
import cz.tul.comm.exceptions.ConnectionExceptionCause;
import cz.tul.comm.history.History;
import cz.tul.comm.history.HistoryManager;
import cz.tul.comm.history.sorting.DefaultSorter;
import cz.tul.comm.job.JobCount;
import cz.tul.comm.job.JobMessageHeaders;
import cz.tul.comm.job.client.AssignmentListener;
import cz.tul.comm.job.client.ClientJobManagerImpl;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.messaging.SystemMessageHeaders;
import cz.tul.comm.persistence.ClientSettings;
import cz.tul.comm.socket.IDFilter;
import cz.tul.comm.socket.ListenerRegistrator;
import cz.tul.comm.socket.ServerSocket;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
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
    private static final int TIMEOUT = 5000;    

    /**
     * Create and initialize new instance of client at given port.
     *
     * @param port target listening port
     * @return new Client instance
     */
    public static Client initNewClient(final int port) throws IOException {
        ClientImpl result = new ClientImpl();
        result.start(port);
        log.log(Level.INFO, "New client created on port {0}", port);

        return result;
    }

    /**
     *
     * @return new client instance on default port
     */
    public static Client initNewClient() {        
        Client c = null;
        int port = Constants.DEFAULT_PORT;
        
        while (c == null && port < 65535) {
            try {
                c = initNewClient(++port);
            } catch (IOException ex) {
                log.log(Level.WARNING, "Error initializing server on port " + (port - 1), ex);
            }

        }
        
        if (c == null) {
            log.log(Level.WARNING, "Error initializing client, no free port found");            
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
                try {
                    if (comm != null) {
                        sendDataToServer(new Message(Constants.ID_SYS_MSG, SystemMessageHeaders.LOGOUT, comm.getSourceId()));
                    }
                } catch (ConnectionException ex) {
                    log.warning("Server connection timed out.");
                }
                stopService();
            }
        }));
    }

    @Override
    public boolean registerToServer(final InetAddress address) throws ConnectionException {
        return registerToServer(address, Constants.DEFAULT_PORT);
    }

    @Override
    public boolean registerToServer(final InetAddress address, final int port) throws ConnectionException {
        final CommunicatorInner oldComm = comm;

        log.log(Level.CONFIG, "Registering new server IP and port - {0}:{1}", new Object[]{address.getHostAddress(), port});
        boolean result = false;
        comm = CommunicatorImpl.initNewCommunicator(address, port);
        comm.setTargetId(Constants.ID_SERVER);
        comm.registerHistory(history);
        final Message login = new Message(Constants.ID_SYS_MSG, SystemMessageHeaders.LOGIN, serverSocket.getPort());
        try {
            final Object id = comm.sendData(login);
            if (id instanceof UUID) {
                comm.setSourceId(((UUID) id));
                result = true;
                log.log(Level.INFO, "Client has been registered to new server, new ID has been received - {0}", comm.getSourceId());
            } else {
                comm = oldComm;
                log.log(Level.WARNING, "Invalid response received - {0}", id.toString());
            }
        } catch (ConnectionException ex) {
            comm = oldComm;
            throw ex;
        }

        return result;
    }

    @Override
    public void setServerInfo(final InetAddress address, final int port, final UUID clientId) {
        comm = CommunicatorImpl.initNewCommunicator(address, port);
        comm.setTargetId(Constants.ID_SERVER);
        comm.setSourceId(clientId);
        comm.registerHistory(history);
    }

    @Override
    public void deregisterFromServer() throws ConnectionException {
        final Message m = new Message(SystemMessageHeaders.LOGOUT, comm.getTargetId());
        sendDataToServer(m);
        comm = null;
    }

    @Override
    public boolean isServerUp() {
        boolean serverStatus = false;
        if (comm != null) {
            Status s = comm.getStatus();
            serverStatus = !s.equals(Status.OFFLINE);
        }
        log.log(Level.INFO, "Is server running - {0}", serverStatus);
        return serverStatus;
    }

    @Override
    public Object sendDataToServer(final Object data) throws ConnectionException {
        return sendDataToServer(data, TIMEOUT);
    }

    @Override
    public Object sendDataToServer(final Object data, final int timeout) throws ConnectionException {
        if (data != null) {
            log.log(Level.INFO, "Sending data to server - {0}", data.toString());
        } else {
            log.log(Level.INFO, "Sending NULL data to server.");
        }
        if (comm == null) {
            throw new ConnectionException(ConnectionExceptionCause.CONNECTION_ERROR);
        } else {
            return comm.sendData(data, TIMEOUT);
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

        csm = new SystemMessageHandler(this, this);
        getListenerRegistrator().setIdListener(Constants.ID_SYS_MSG, csm);

        jm = new ClientJobManagerImpl(this);
        getListenerRegistrator().setIdListener(Constants.ID_JOB_MANAGER, jm);
        
        if (ComponentSwitches.useClientDiscovery) {
            if (sdd != null) {
                sdd.start();
            } else if (ComponentSwitches.useClientAutoConnectLocalhost && !isServerUp()) {
                log.info("Could not init server discovery, trying to connect to local host.");
                try {
                    registerToServer(InetAddress.getLoopbackAddress(), Constants.DEFAULT_PORT);
                } catch (ConnectionException ex) {
                    log.info("Could not reach server at localhost on default port.");
                }
            }
        }
    }

    @Override
    public void stopService() {
        if (serverSocket != null) {
            serverSocket.stopService();
        }
        if (sdd != null) {
            sdd.stopService();
        }
        log.fine("Client has been stopped.");
    }

    @Override
    public boolean isIdAllowed(UUID id) {
        if (comm == null && id.equals(Constants.ID_SERVER)) {
            return true;
        }

        if (comm == null) {
            return false;
        }

        final UUID commId = comm.getTargetId();
        if (commId == null) {
            return true;
        } else {
            return comm.getTargetId().equals(id);
        }
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
    public boolean isTargetIdValid(UUID id) {
        if (id != null) {
            if (comm != null) {
                return id.equals(comm.getSourceId());
            } else {
                return true;
            }
        } else {
            if (comm == null) {
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public UUID getLocalID() {
        if (comm != null) {
            return comm.getSourceId();
        } else {
            return null;
        }
    }

    @Override
    public boolean setMaxNumberOfConcurrentAssignments(final int assignmentCount) {
        final Message m = new Message(
                Constants.ID_JOB_MANAGER,
                JobMessageHeaders.JOB_COUNT,
                new JobCount(getLocalID(), assignmentCount));

        try {
            final Object response = sendDataToServer(m);
            return GenericResponses.OK.equals(response);
        } catch (ConnectionException ex) {
            log.log(Level.WARNING, "Communication with server failed - {0}", ex);
            return false;
        }

    }
    
    @Override
    public void loadSettings(final File settingsFile) {
        ClientSettings.deserialize(settingsFile, this);
    }
}
