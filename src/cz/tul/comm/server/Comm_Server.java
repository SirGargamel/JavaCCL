package cz.tul.comm.server;

import cz.tul.comm.ComponentSwitches;
import cz.tul.comm.Constants;
import cz.tul.comm.persistence.ServerSettings;
import cz.tul.comm.IService;
import cz.tul.comm.client.Comm_Client;
import cz.tul.comm.history.History;
import cz.tul.comm.history.IHistoryManager;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.gui.UserLogging;
import cz.tul.comm.messaging.job.Job;
import cz.tul.comm.messaging.job.JobManager;
import cz.tul.comm.server.daemons.ClientDiscoveryDaemon;
import cz.tul.comm.server.daemons.ClientStatusDaemon;
import cz.tul.comm.socket.IListenerRegistrator;
import cz.tul.comm.socket.ServerSocket;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class enclosing server-client communication. Handles custom data sending to
 * given client, whole job computation and client status monitoring.
 *
 * @author Petr Ječmen
 */
public final class Comm_Server implements IService {

    private static final Logger log = Logger.getLogger(Comm_Server.class.getName());
    private final ClientDB clients;
    private final ServerSocket serverSocket;
    private final IHistoryManager history;
    private final ClientStatusDaemon clientStatusDaemon;
    private final JobManager jobManager;
    private ClientDiscoveryDaemon cdd;

    private Comm_Server(final int port) throws IOException {
        history = new History();

        clients = new ClientDB();

        serverSocket = ServerSocket.createServerSocket(port);
        serverSocket.registerHistory(history);

        if (ComponentSwitches.useClientStatus) {
            clientStatusDaemon = new ClientStatusDaemon(clients, serverSocket);
        } else {
            clientStatusDaemon = null;
        }

        getListenerRegistrator().addMessageObserver(new SystemMessagesHandler(clients));

        if (ComponentSwitches.useClientDiscovery) {
            try {
                cdd = new ClientDiscoveryDaemon(clients);
            } catch (SocketException ex) {
                log.log(Level.WARNING, "Failed to create ClientDiscoveryDaemon", ex);
            }
        }

        jobManager = new JobManager(clients, serverSocket);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                ServerSettings.serialize(clients);
            }
        }));
    }

    /**
     * Register new client communicationg on given IP and on default port.
     *
     * @param adress client IP
     * @return
     */
    public Communicator registerClient(final InetAddress adress) {
        log.log(Level.INFO, "Registering new client on IP{0} on default port", adress);
        return clients.registerClient(adress, Comm_Client.PORT);
    }

    /**
     *
     * @param address
     * @return
     */
    public Communicator getClient(final InetAddress address) {
        return clients.getClient(address, Comm_Client.PORT);
    }

    /**
     * Export history as is to XML file.
     *
     * @return true for successfull export.
     */
    public boolean exportHistory() {
        log.info("Exporting history.");
        return history.export(new File(""), null);
    }

    /**
     * @return history manager for this client
     */
    public IHistoryManager getHistory() {
        return history;
    }

    /**
     *
     * @return
     */
    public IClientManager getClientManager() {
        return clients;
    }

    /**
     * Interface for registering new data listeners.
     *
     * @return
     */
    public IListenerRegistrator getListenerRegistrator() {
        return serverSocket;
    }

    public void assignDataStorage(final IDataStorage dataStorage) {
        jobManager.setDataStorage(dataStorage);
    }

    public Job submitJob(final Object task) {
        return jobManager.submitJob(task);
    }

    /**
     * Create and initialize new instance of server.
     *
     * @param port
     * @return new instance of Comm_Server
     */
    public static Comm_Server initNewServer(final int port) throws IOException {
        final Comm_Server result = new Comm_Server(port);
        result.start();
        log.log(Level.INFO, "New server created on port {0}", port);

        return result;
    }

    /**
     *
     * @return
     */
    public static Comm_Server initNewServer() {
        Comm_Server s = null;

        try {
            s = initNewServer(Constants.DEFAULT_PORT);
        } catch (IOException ex) {
            UserLogging.showErrorToUser("Error initializing server on default port.");
            log.log(Level.SEVERE, "Error initializing server on default port", ex);
        }

        return s;
    }

    void start() {
        if (ComponentSwitches.useSettings && !ServerSettings.deserialize(clients)) {
            UserLogging.showWarningToUser("Error reading settings file, using default ones.");
        }
        if (clientStatusDaemon != null) {
            clientStatusDaemon.start();
        }
        if (cdd != null) {
            cdd.start();
        }
        jobManager.start();
    }

    @Override
    public void stopService() {
        if (clientStatusDaemon != null) {
            clientStatusDaemon.stopService();
        }
        if (cdd != null) {
            cdd.stopService();
        }
        
        jobManager.stopService();
        serverSocket.stopService();
        
        log.fine("Server has been stopped.");
    }
}
