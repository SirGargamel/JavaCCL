package cz.tul.comm.server;

import cz.tul.comm.ComponentSwitches;
import cz.tul.comm.Constants;
import cz.tul.comm.IService;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.history.History;
import cz.tul.comm.history.HistoryManager;
import cz.tul.comm.job.Job;
import cz.tul.comm.job.JobManager;
import cz.tul.comm.job.JobManagerImpl;
import cz.tul.comm.persistence.ServerSettings;
import cz.tul.comm.server.daemons.ClientDiscoveryDaemon;
import cz.tul.comm.server.daemons.ClientStatusDaemon;
import cz.tul.comm.socket.ListenerRegistrator;
import cz.tul.comm.socket.ServerSocket;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class enclosing server-client communication. Handles custom data sending to
 * given client, whole job computation and client status monitoring.
 *
 * @author Petr Ječmen
 */
public final class Comm_Server implements IService, Server {

    private static final Logger log = Logger.getLogger(Comm_Server.class.getName());

    /**
     * Create and initialize new instance of server.
     *
     * @param port server port (muse be valid port nuber between 0 and 65535)
     * @return new instance of Comm_Server
     * @throws IOException error opening socket on given port
     */
    public static Server initNewServer(final int port) throws IOException {
        final Comm_Server result = new Comm_Server(port);
        result.start();
        log.log(Level.INFO, "New server created on port {0}", port);

        return result;
    }

    /**
     *
     * @return
     */
    public static Server initNewServer() {
        Server s = null;

        try {
            s = initNewServer(Constants.DEFAULT_PORT);
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Error initializing server on default port", ex);
        }

        return s;
    }
    private final ClientDB clients;
    private final ServerSocket serverSocket;
    private final HistoryManager history;
    private final ClientStatusDaemon clientStatusDaemon;
    private final JobManagerImpl jobManager;
    private ClientDiscoveryDaemon cdd;

    private Comm_Server(final int port) throws IOException {
        history = new History();

        clients = new ClientDB();

        serverSocket = ServerSocket.createServerSocket(port, clients);
        serverSocket.registerHistory(history);

        if (ComponentSwitches.useClientStatus) {
            clientStatusDaemon = new ClientStatusDaemon(clients);
        } else {
            clientStatusDaemon = null;
        }
        
        jobManager = new JobManagerImpl(clients, serverSocket);

        getListenerRegistrator().addMessageObserver(new SystemMessagesHandler(clients, jobManager));

        if (ComponentSwitches.useClientDiscovery) {
            try {
                cdd = new ClientDiscoveryDaemon(clients);
            } catch (SocketException ex) {
                log.log(Level.WARNING, "Failed to create ClientDiscoveryDaemon", ex);
            }
        }        

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                stopService();
            }
        }));
    }

    /**
     * Register new client communicationg on given IP and on default port.
     *
     * @param adress client IP
     * @return
     */
    @Override
    public Communicator registerClient(final InetAddress adress) {
        log.log(Level.INFO, "Registering new client on IP{0} on default port", adress);
        return clients.registerClient(adress, Constants.DEFAULT_PORT);
    }

    /**
     *
     * @param address
     * @return
     */
    @Override
    public Communicator getClient(final InetAddress address) {
        return clients.getClient(address, Constants.DEFAULT_PORT);
    }

    /**
     * Export history as is to XML file.
     *
     * @return true for successfull export.
     */
    @Override
    public boolean exportHistory() {
        log.info("Exporting history.");
        return history.export(new File(""), null);
    }

    /**
     * @return history manager for this client
     */
    @Override
    public HistoryManager getHistory() {
        return history;
    }

    /**
     *
     * @return
     */
    @Override
    public ClientManager getClientManager() {
        return clients;
    }

    /**
     * Interface for registering new data listeners.
     *
     * @return
     */
    @Override
    public ListenerRegistrator getListenerRegistrator() {
        return serverSocket;
    }

    @Override
    public JobManager getJobManager() {
        return jobManager;
    }

    /**
     * @param dataStorage class hnadling data requests
     */
    @Override
    public void assignDataStorage(final DataStorage dataStorage) {
        jobManager.setDataStorage(dataStorage);
    }

    /**
     * Submit new job for computation
     *
     * @param task jobs task
     * @return interface for job control and result obtaining
     */
    @Override
    public Job submitJob(final Object task) {
        return jobManager.submitJob(task);
    }

    void start() {
        if (ComponentSwitches.useSettings) {
            if (!ServerSettings.deserialize(clients)) {
                log.warning("Error loading server settings.");
            }
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    ServerSettings.serialize(clients);
                }
            }));
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

    @Override
    public Communicator getClient(UUID id) {
        return clients.getClient(id);
    }
}
