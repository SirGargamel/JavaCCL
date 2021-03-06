package cz.tul.javaccl.server;

import cz.tul.javaccl.ComponentManager;
import cz.tul.javaccl.GlobalConstants;
import cz.tul.javaccl.communicator.Communicator;
import cz.tul.javaccl.exceptions.ConnectionException;
import cz.tul.javaccl.history.History;
import cz.tul.javaccl.history.HistoryManager;
import cz.tul.javaccl.job.server.Job;
import cz.tul.javaccl.job.server.ServerJobManager;
import cz.tul.javaccl.job.server.ServerJobManagerImpl;
import cz.tul.javaccl.messaging.Message;
import cz.tul.javaccl.messaging.SystemMessageHeaders;
import cz.tul.javaccl.persistence.ServerSettings;
import cz.tul.javaccl.discovery.ClientDiscoveryDaemon;
import cz.tul.javaccl.persistence.ClientSettings;
import cz.tul.javaccl.persistence.SimpleXMLSettingsFile;
import cz.tul.javaccl.persistence.XmlNodes;
import cz.tul.javaccl.socket.IDFilter;
import cz.tul.javaccl.socket.ListenerRegistrator;
import cz.tul.javaccl.socket.ServerSocket;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Observable;
import java.util.Observer;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class enclosing server-to-client communication. Handles custom data sending
 * to given client, whole job computation and client status monitoring.
 *
 * @author Petr Ječmen
 */
public final class ServerImpl extends Server implements Observer {

    private static final Logger LOG = Logger.getLogger(ServerImpl.class.getName());
    private final ClientDB clients;
    private final ServerSocket serverSocket;
    private final HistoryManager history;
    private final ServerJobManagerImpl jobManager;
    private ClientDiscoveryDaemon cdd;

    /**
     * Create and initialize new instance of server.
     *
     * @param port server port (muse be valid port nuber between 0 and 65535)
     * @return new instance of ServerImpl
     * @throws IOException error opening socket on given port
     */
    public static Server initNewServer(final int port) throws IOException {
        final ServerImpl result = new ServerImpl(port);
        result.start();
        LOG.log(Level.INFO, "New server created on port {0}", port);

        return result;
    }

    /**
     * Create and initialize new instance of server on default port.
     *
     * @return new instance of ServerImpl
     */
    public static Server initNewServer() {
        Server server = null;
        int port = GlobalConstants.DEFAULT_PORT;

        while (server == null && port < 65535) {
            try {
                server = initNewServer(port++);
            } catch (IOException ex) {
                LOG.log(Level.WARNING, "Error initializing server on port " + (port - 1));
                LOG.log(Level.FINE, "Error initializing server on port " + (port - 1), ex);
            }
        }

        if (server == null) {
            LOG.log(Level.WARNING, "Error initializing server, no free port found");
        }

        return server;
    }

    ServerImpl(final int port) throws IOException {
        super();
        history = new History();

        clients = new ClientDB(getId());
        clients.registerHistory(history);

        serverSocket = ServerSocket.createServerSocket(port, clients, clients);
        serverSocket.registerHistory(history);

        jobManager = new ServerJobManagerImpl(clients, serverSocket);
        getListenerRegistrator().setIdListener(GlobalConstants.ID_JOB_MANAGER, jobManager);

        getListenerRegistrator().setIdListener(GlobalConstants.ID_SYS_MSG, new SystemMessagesHandler(clients, getId()));

        try {
            cdd = new ClientDiscoveryDaemon(clients);
        } catch (SocketException ex) {
            LOG.log(Level.WARNING, "Failed to create ClientDiscoveryDaemon");
            LOG.log(Level.FINE, "Failed to create ClientDiscoveryDaemon", ex);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                stopService();
            }
        }));
    }

    @Override
    public Communicator registerClient(final InetAddress adress) throws ConnectionException {
        LOG.log(Level.INFO, "Registering new client on IP " + adress.getHostAddress() + " on default port");
        return clients.registerClient(adress, GlobalConstants.DEFAULT_PORT);
    }

    @Override
    public Communicator getClient(final InetAddress address) {
        Communicator result = clients.getClient(address, GlobalConstants.DEFAULT_PORT);

        if (result == null) {
            for (Communicator cc : clients.getClients()) {
                if (cc.getAddress().equals(address)) {
                    result = cc;
                    break;
                }
            }
        }

        return result;
    }

    @Override
    public boolean exportHistory() {
        LOG.info("Exporting history.");
        return history.export(new File(""), null);
    }

    @Override
    public HistoryManager getHistory() {
        return history;
    }

    @Override
    public ClientManager getClientManager() {
        return clients;
    }

    @Override
    public ListenerRegistrator getListenerRegistrator() {
        return serverSocket;
    }

    @Override
    public ServerJobManager getJobManager() {
        return jobManager;
    }

    @Override
    public void assignDataStorage(final DataStorage dataStorage) {
        jobManager.setDataStorage(dataStorage);
    }

    @Override
    public Job submitJob(final Object task) throws IllegalArgumentException {
        return jobManager.submitJob(task);
    }

    void start() {
        clients.addObserver(this);
        if (cdd != null) {
            cdd.start();
        }
        jobManager.start();
    }

    @Override
    public void stopService() {
        for (Communicator comm : clients.getClients()) {
            try {
                comm.sendData(new Message(GlobalConstants.ID_SYS_MSG, SystemMessageHeaders.LOGOUT, null));
            } catch (ConnectionException ex) {
                LOG.warning("Client connection timed out.");
            }
        }

        if (cdd != null) {
            try {
                cdd.stopService();
            } catch (Exception ex) {
                // error closing some resource, ignore
            }
        }

        try {
            jobManager.stopService();
        } catch (Exception ex) {
            // error closing some resource, ignore
        }
        try {
            serverSocket.stopService();
        } catch (Exception ex) {
            // error closing some resource, ignore
        }

        LOG.fine("Server has been stopped.");
    }

    @Override
    public Communicator getClient(UUID id) {
        return clients.getClient(id);
    }

    @Override
    public boolean loadSettings(final File settingsFile) {
        return ServerSettings.deserialize(settingsFile, clients);
    }

    @Override
    public boolean saveSettings(final File settingsFile) {
        return ServerSettings.serialize(settingsFile, clients);
    }

    @Override
    public void update(Observable o, Object arg) {
        setChanged();
        notifyObservers(arg);
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
        cdd.enable(enable);
    }

    @Override
    public boolean generateClientSettings(final File settingsFile) {
        LOG.log(Level.FINE, "Generating client settings.");

        final SimpleXMLSettingsFile xml = new SimpleXMLSettingsFile();

        boolean result = false;
        try {            
            final Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();

            for (NetworkInterface n : Collections.list(nets)) {
                if (n.isUp()) {
                    for (InetAddress address : Collections.list(n.getInetAddresses())) {
                        xml.addField(
                                XmlNodes.SERVER.toString(),
                                ClientSettings.composeServerAddress(address, serverSocket.getPort()));
                    }
                }
            }
            
            result = xml.storeXML(settingsFile);
        } catch (SocketException ex) {
            LOG.log(Level.WARNING, "Error listing available network interfaces.", settingsFile.getAbsolutePath());
            LOG.log(Level.FINE, "Error listing available network interfaces.", ex);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Error accessing client settings at {0}.", settingsFile.getAbsolutePath());
            LOG.log(Level.FINE, "Error accessing client settings at " + settingsFile.getAbsolutePath() + ".", ex);
        }

        return result;
    }
}
