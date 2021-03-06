package cz.tul.javaccl.client;

import cz.tul.javaccl.ComponentManager;
import cz.tul.javaccl.discovery.ServerDiscoveryDaemon;
import cz.tul.javaccl.socket.ClientLister;
import cz.tul.javaccl.GlobalConstants;
import cz.tul.javaccl.GenericResponses;
import cz.tul.javaccl.communicator.Communicator;
import cz.tul.javaccl.communicator.CommunicatorImpl;
import cz.tul.javaccl.communicator.CommunicatorInner;
import cz.tul.javaccl.exceptions.ConnectionException;
import cz.tul.javaccl.exceptions.ConnectionExceptionCause;
import cz.tul.javaccl.history.History;
import cz.tul.javaccl.history.HistoryManager;
import cz.tul.javaccl.history.sorting.DefaultSorter;
import cz.tul.javaccl.job.ClientJobSettings;
import cz.tul.javaccl.job.JobConstants;
import cz.tul.javaccl.job.client.AssignmentListener;
import cz.tul.javaccl.job.client.ClientJobManagerImpl;
import cz.tul.javaccl.messaging.Message;
import cz.tul.javaccl.messaging.SystemMessageHeaders;
import cz.tul.javaccl.persistence.ClientSettings;
import cz.tul.javaccl.persistence.Timeout;
import cz.tul.javaccl.socket.IDFilter;
import cz.tul.javaccl.socket.ListenerRegistrator;
import cz.tul.javaccl.socket.ServerSocket;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Observable;
import java.util.Observer;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class enclosing client to server communication. Allows data sending and has a
 * registry for message handling.
 *
 * @author Petr Ječmen
 */
public class ClientImpl extends Client implements ServerInterface, IDFilter, ClientLister, Observer {

    private static final Logger LOG = Logger.getLogger(ClientImpl.class.getName());
    private ServerSocket serverSocket;
    private final HistoryManager history;
    private CommunicatorInner comm;
    private SystemMessageHandler csm;
    private ClientJobManagerImpl jm;
    private ServerDiscoveryDaemon sdd;
    private int concurentJobCount, jobCountBackup;
    private int jobComplexity;

    /**
     * Create and initialize new instance of client at given port.
     *
     * @param port target listening port
     * @return new Client instance
     * @throws IOException target port is already in use
     */
    public static Client initNewClient(final int port) throws IOException {
        final ClientImpl result = new ClientImpl();
        result.start(port);
        LOG.log(Level.INFO, "New client created on port {0}", port);

        return result;
    }

    /**
     *
     * @return new client instance on default port
     */
    public static Client initNewClient() {
        Client result = null;
        int port = GlobalConstants.DEFAULT_PORT;

        while (result == null && port < 65535) {
            try {
                result = initNewClient(++port);
            } catch (IOException ex) {                
                LOG.log(Level.WARNING, "Error initializing client on port {0}", (port - 1));
                LOG.log(Level.FINE, "Error initializing client on port " + (port - 1), ex);
            }

        }

        if (result == null) {
            LOG.log(Level.WARNING, "Error initializing client, no free port found");
        }

        return result;
    }

    ClientImpl() {
        super();
        concurentJobCount = 1;
        jobCountBackup = 1;
        jobComplexity = JobConstants.DEFAULT_COMPLEXITY;

        history = new History();

        try {
            sdd = new ServerDiscoveryDaemon(this);
        } catch (SocketException ex) {
            LOG.log(Level.FINE, "Failed to initiate server discovery daemon.", ex);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                stopService();
            }
        }));
    }

    @Override
    public boolean registerToServer(final InetAddress address) throws ConnectionException {
        return registerToServer(address, GlobalConstants.DEFAULT_PORT);
    }

    @Override
    public boolean registerToServer(final InetAddress address, final int port) throws ConnectionException {
        final CommunicatorInner oldComm = comm;

        LOG.log(Level.INFO, "Registering new server IP and port - " + address.getHostAddress() + ":" + port);
        boolean result = false;
        comm = CommunicatorImpl.initNewCommunicator(address, port, getId());
        comm.registerHistory(history);
        final Message login = new Message(GlobalConstants.ID_SYS_MSG, SystemMessageHeaders.LOGIN, serverSocket.getPort());
        try {
            final Object id = comm.sendData(login);
            if (id instanceof UUID) {
                comm.setTargetId(((UUID) id));
                result = true;
                LOG.log(Level.INFO, "Client has been registered to new server (ID " + id + ")");
                notifyChange(REGISTER, new Object[]{address, port, id});
                setMaxNumberOfConcurrentAssignments(concurentJobCount);
                setMaxJobComplexity(jobComplexity);
            } else {
                comm = oldComm;
                LOG.log(Level.WARNING, "Invalid response received - " + id.toString());
                LOG.log(Level.INFO, "Registration failed, server sent invalid data.");
            }
        } catch (ConnectionException ex) {
            comm = oldComm;
            LOG.log(Level.INFO, "Registration failed - " + ex.getExceptionCause());
            throw ex;
        }

        return result;
    }

    @Override
    public void setServerInfo(final InetAddress address, final int port, final UUID serverId) {
        comm = CommunicatorImpl.initNewCommunicator(address, port, getId());
        comm.setTargetId(serverId);
        comm.registerHistory(history);
        setMaxNumberOfConcurrentAssignments(concurentJobCount);
        setMaxJobComplexity(jobComplexity);
    }

    @Override
    public void deregisterFromServer() throws ConnectionException {
        if (comm != null) {
            final Message m = new Message(SystemMessageHeaders.LOGOUT, comm.getTargetId());
            sendDataToServer(m);
            comm = null;
            notifyChange(DEREGISTER, null);
        }
    }

    @Override
    public boolean isServerUp() {
        return comm == null ? false : comm.isOnline();
    }

    @Override
    public Object sendDataToServer(final Object data) throws ConnectionException {
        return sendDataToServer(data, Timeout.getTimeout(Timeout.TimeoutType.MESSAGE));
    }

    @Override
    public Object sendDataToServer(final Object data, final int timeout) throws ConnectionException {
        if (data != null) {
            LOG.log(Level.INFO, "Sending data to server - " + data.toString());
        } else {
            LOG.log(Level.INFO, "Sending NULL data to server.");
        }
        if (comm == null) {
            throw new ConnectionException(ConnectionExceptionCause.CONNECTION_ERROR);
        } else {
            return comm.sendData(data, timeout);
        }
    }

    @Override
    public boolean exportHistory() {
        LOG.info("Exporting histry to default location with no sorting.");
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

    void start(final int port) throws IOException {
        serverSocket = ServerSocket.createServerSocket(port, this, this);
        serverSocket.registerHistory(history);

        csm = new SystemMessageHandler(this, this);
        csm.addObserver(this);
        getListenerRegistrator().setIdListener(GlobalConstants.ID_SYS_MSG, csm);

        jm = new ClientJobManagerImpl(this);
        getListenerRegistrator().setIdListener(GlobalConstants.ID_JOB_MANAGER, jm);

        if (sdd != null) {
            sdd.start();
        } else if (!isServerUp()) {
            LOG.fine("Could not init server discovery, trying to connect to local host.");
            try {
                registerToServer(GlobalConstants.IP_LOOPBACK, GlobalConstants.DEFAULT_PORT);
            } catch (ConnectionException ex) {
                LOG.fine("Could not reach server at localhost on default port.");
            }
        }
    }

    @Override
    public void stopService() {
        try {
            deregisterFromServer();
        } catch (ConnectionException ex) {
            LOG.warning("Server could not be reached for deregistration.");
        }
        disconnectFromServer();

        try {
            if (serverSocket != null) {
                serverSocket.stopService();
            }
        } catch (Exception ex) {
            // error closing some resource, ignore
        }
        try {
            if (sdd != null) {
                sdd.stopService();
            }
        } catch (Exception ex) {
            // error closing some resource, ignore
        }
        LOG.info("Client has been stopped.");
    }

    @Override
    public boolean isIdAllowed(UUID id) {
        boolean result = false;

        if (comm != null && id != null) {
            result = id.equals(comm.getTargetId());
        }

        return result;
    }

    @Override
    public int getLocalSocketPort() {
        return serverSocket.getPort();
    }

    @Override
    public Collection<Communicator> getClients() {
        final Collection<Communicator> result = new ArrayList<Communicator>(1);
        result.add(comm);
        return result;
    }

    @Override
    public boolean isTargetIdValid(UUID id) {
        return getLocalID().equals(id);
    }

    @Override
    public UUID getLocalID() {
        return getId();
    }

    @Override
    public boolean setMaxNumberOfConcurrentAssignments(final int assignmentCount) {
        concurentJobCount = assignmentCount;
        this.jm.setMaxNumberOfConcurrentAssignments(assignmentCount);
        boolean result = false;
        if (isServerUp()) {
            final Message m = new Message(
                    GlobalConstants.ID_JOB_MANAGER,
                    JobConstants.JOB_CLIENT_SETTINGS,
                    new ClientJobSettings(getLocalID(), JobConstants.JOB_COUNT, assignmentCount));

            try {
                final Object response = sendDataToServer(m);
                result = GenericResponses.OK.equals(response);
            } catch (ConnectionException ex) {
                LOG.log(Level.WARNING, "Communication with server failed - " + ex.getExceptionCause());
            }
        }

        if (result) {
            LOG.log(Level.FINE, "Concurrent job count set on server to " + assignmentCount);
        } else {
            LOG.log(Level.FINE, "Failed to update concurrent job count on server.");
        }
        return result;
    }

    @Override
    public boolean setMaxJobComplexity(int maxJobComplexity) {
        this.jobComplexity = maxJobComplexity;
        boolean result = false;
        if (isServerUp()) {
            final Message m = new Message(
                    GlobalConstants.ID_JOB_MANAGER,
                    JobConstants.JOB_CLIENT_SETTINGS,
                    new ClientJobSettings(getLocalID(), JobConstants.JOB_COMPLEXITY, maxJobComplexity));

            try {
                final Object response = sendDataToServer(m);
                result = GenericResponses.OK.equals(response);
            } catch (ConnectionException ex) {
                LOG.log(Level.WARNING, "Communication with server failed - " + ex.getExceptionCause());
            }
        }

        if (result) {
            LOG.log(Level.FINE, "Maximal job complexity set on server to " + maxJobComplexity);
        } else {
            LOG.log(Level.FINE, "Failed to update maximal job complexity on server.");
        }
        return result;
    }

    @Override
    public boolean loadSettings(final File settingsFile) {
        return ClientSettings.deserialize(settingsFile, this);
    }

    @Override
    public boolean saveSettings(final File settingsFile) {
        return ClientSettings.serialize(settingsFile, comm);
    }

    @Override
    public void disconnectFromServer() {
        comm = null;
    }

    @Override
    public void update(Observable o, Object arg) {
        setChanged();
        notifyObservers(arg);
    }

    @Override
    public void enableClient(boolean enable) {
        if (enable) {
            jobCountBackup = jobComplexity;
            setMaxNumberOfConcurrentAssignments(0);
        } else {
            setMaxNumberOfConcurrentAssignments(jobCountBackup);
        }
    }

    @Override
    public ComponentManager getComponentManager() {
        return this;
    }

    @Override
    public void setIdFilter(IDFilter filter) {
        serverSocket.setIdFilter(filter);
    }

    @Override
    public void enableDiscoveryDaemon(boolean enable) {
        sdd.enable(enable);
    }
}
