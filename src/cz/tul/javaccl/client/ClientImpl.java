package cz.tul.javaccl.client;

import cz.tul.javaccl.discovery.ServerDiscoveryDaemon;
import cz.tul.javaccl.socket.ClientLister;
import cz.tul.javaccl.Constants;
import cz.tul.javaccl.GenericResponses;
import cz.tul.javaccl.IService;
import cz.tul.javaccl.communicator.Communicator;
import cz.tul.javaccl.communicator.CommunicatorImpl;
import cz.tul.javaccl.communicator.CommunicatorInner;
import cz.tul.javaccl.exceptions.ConnectionException;
import cz.tul.javaccl.exceptions.ConnectionExceptionCause;
import cz.tul.javaccl.history.History;
import cz.tul.javaccl.history.HistoryManager;
import cz.tul.javaccl.history.sorting.DefaultSorter;
import cz.tul.javaccl.job.JobCount;
import cz.tul.javaccl.job.JobMessageHeaders;
import cz.tul.javaccl.job.client.AssignmentListener;
import cz.tul.javaccl.job.client.ClientJobManagerImpl;
import cz.tul.javaccl.messaging.Message;
import cz.tul.javaccl.messaging.SystemMessageHeaders;
import cz.tul.javaccl.persistence.ClientSettings;
import cz.tul.javaccl.socket.IDFilter;
import cz.tul.javaccl.socket.ListenerRegistrator;
import cz.tul.javaccl.socket.ServerSocket;
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
public class ClientImpl extends Client implements IService, ServerInterface, IDFilter, ClientLister {

    private static final Logger log = Logger.getLogger(ClientImpl.class.getName());
    private ServerSocket serverSocket;
    private final HistoryManager history;
    private CommunicatorInner comm;
    private SystemMessageHandler csm;
    private ClientJobManagerImpl jm;
    private ServerDiscoveryDaemon sdd;
    private int concurentJobCount;

    ClientImpl() {
        concurentJobCount = 1;

        history = new History();

        try {
            sdd = new ServerDiscoveryDaemon(this);
        } catch (SocketException ex) {
            log.log(Level.FINE, "Failed to initiate server discovery daemon.", ex);
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
        return registerToServer(address, Constants.DEFAULT_PORT);
    }

    @Override
    public boolean registerToServer(final InetAddress address, final int port) throws ConnectionException {
        final CommunicatorInner oldComm = comm;

        log.log(Level.INFO, "Registering new server IP and port - " + address.getHostAddress() + ":" + port);
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
                log.log(Level.INFO, "Client has been registered to new server, new ID has been received - " + comm.getSourceId());
                setMaxNumberOfConcurrentAssignments(concurentJobCount);
            } else {
                comm = oldComm;
                log.log(Level.WARNING, "Invalid response received - " + id.toString());
                log.log(Level.INFO, "Registration failed");
            }
        } catch (ConnectionException ex) {
            comm = oldComm;
            log.log(Level.INFO, "Registration failed");
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
        setMaxNumberOfConcurrentAssignments(concurentJobCount);
    }

    @Override
    public void deregisterFromServer() throws ConnectionException {
        if (comm != null) {
            final Message m = new Message(SystemMessageHeaders.LOGOUT, comm.getTargetId());
            sendDataToServer(m);
            comm = null;
        }
    }

    @Override
    public boolean isServerUp() {
        boolean serverStatus = false;
        if (comm != null) {
            serverStatus = comm.isOnline();
        }
        return serverStatus;
    }

    @Override
    public Object sendDataToServer(final Object data) throws ConnectionException {
        return sendDataToServer(data, Constants.DEFAULT_TIMEOUT);
    }

    @Override
    public Object sendDataToServer(final Object data, final int timeout) throws ConnectionException {
        if (data != null) {
            log.log(Level.INFO, "Sending data to server - " + data.toString());
        } else {
            log.log(Level.INFO, "Sending NULL data to server.");
        }
        if (comm == null) {
            throw new ConnectionException(ConnectionExceptionCause.CONNECTION_ERROR);
        } else {
            return comm.sendData(data, timeout);
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

    void start(final int port) throws IOException {
        serverSocket = ServerSocket.createServerSocket(port, this, this);
        serverSocket.registerHistory(history);

        csm = new SystemMessageHandler(this, this);
        getListenerRegistrator().setIdListener(Constants.ID_SYS_MSG, csm);

        jm = new ClientJobManagerImpl(this);
        getListenerRegistrator().setIdListener(Constants.ID_JOB_MANAGER, jm);

        if (sdd != null) {
            sdd.start();
        } else if (!isServerUp()) {
            log.fine("Could not init server discovery, trying to connect to local host.");
            try {
                registerToServer(InetAddress.getByName(Constants.IP_LOOPBACK), Constants.DEFAULT_PORT);
            } catch (ConnectionException ex) {
                log.fine("Could not reach server at localhost on default port.");
            }
        }
    }

    @Override
    public void stopService() {
        try {
            deregisterFromServer();
        } catch (ConnectionException ex) {
            log.warning("Server could not be reached for deregistration.");
        }
        disconnectFromServer();

        if (serverSocket != null) {
            serverSocket.stopService();
        }
        if (sdd != null) {
            sdd.stopService();
        }
        log.info("Client has been stopped.");
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
        final Collection<Communicator> result = new ArrayList<Communicator>(1);
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
        boolean result = false;
        if (isServerUp()) {
            final Message m = new Message(
                    Constants.ID_JOB_MANAGER,
                    JobMessageHeaders.JOB_COUNT,
                    new JobCount(getLocalID(), assignmentCount));

            try {
                final Object response = sendDataToServer(m);
                return GenericResponses.OK.equals(response);
            } catch (ConnectionException ex) {
                log.log(Level.WARNING, "Communication with server failed - " + ex.getExceptionCause());
            }
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
}
