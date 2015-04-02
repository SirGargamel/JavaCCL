package cz.tul.javaccl.server;

import cz.tul.javaccl.ComponentManager;
import cz.tul.javaccl.GlobalConstants;
import cz.tul.javaccl.IService;
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
public final class ServerImpl extends Server implements IService, Observer {

    private static final Logger log = Logger.getLogger(ServerImpl.class.getName());
    private final ClientDB clients;
    private final ServerSocket serverSocket;
    private final HistoryManager history;
    private final ServerJobManagerImpl jobManager;
    private ClientDiscoveryDaemon cdd;

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
            log.log(Level.WARNING, "Failed to create ClientDiscoveryDaemon");
            log.log(Level.FINE, "Failed to create ClientDiscoveryDaemon", ex);
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
        log.log(Level.INFO, "Registering new client on IP " + adress.getHostAddress() + " on default port");
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
        log.info("Exporting history.");
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
                log.warning("Client connection timed out.");
            }
        }

        if (cdd != null) {
            cdd.stopService();
        }

        jobManager.stopService();
        serverSocket.stopService();

        log.fine("Server has been stopped.");
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
    public boolean generateClientSettings(File settingsFile) {
        log.log(Level.FINE, "Generating client settings.");

        final SimpleXMLSettingsFile xml = new SimpleXMLSettingsFile();

        boolean result = false;
        try {
            result = xml.storeXML(settingsFile);

            final Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();

            for (NetworkInterface n : Collections.list(nets)) {
                if (n.isUp()) {
                    for (InetAddress address : Collections.list(n.getInetAddresses())) {
                        xml.addField(
                                ClientSettings.FIELD_NAME_SERVER,
                                ClientSettings.composeServerAddress(address, serverSocket.getPort()));
                    }
                }
            }
        } catch (SocketException ex) {
            log.log(Level.WARNING, "Error listing available network interfaces.", settingsFile.getAbsolutePath());
            log.log(Level.FINE, "Error listing available network interfaces.", ex);
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error accessing client settings at {0}.", settingsFile.getAbsolutePath());
            log.log(Level.FINE, "Error accessing client settings at " + settingsFile.getAbsolutePath() + ".", ex);
        }

        return result;
    }
}
