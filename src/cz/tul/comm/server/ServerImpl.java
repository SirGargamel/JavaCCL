package cz.tul.comm.server;

import cz.tul.comm.ComponentSwitches;
import cz.tul.comm.Constants;
import cz.tul.comm.IService;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.exceptions.ConnectionException;
import cz.tul.comm.history.History;
import cz.tul.comm.history.HistoryManager;
import cz.tul.comm.job.server.Job;
import cz.tul.comm.job.server.ServerJobManager;
import cz.tul.comm.job.server.ServerJobManagerImpl;
import cz.tul.comm.persistence.ServerSettings;
import cz.tul.comm.server.daemons.ClientDiscoveryDaemon;
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
 * Class enclosing server-to-client communication. Handles custom data sending
 * to given client, whole job computation and client status monitoring.
 *
 * @author Petr Ječmen
 */
public final class ServerImpl implements IService, Server {

    private static final Logger log = Logger.getLogger(ServerImpl.class.getName());

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
        log.log(Level.INFO, "New server created on port {0}", new Object[]{port});

        return result;
    }

    /**
     * Create and initialize new instance of server on default port.
     *
     * @return new instance of ServerImpl
     */
    public static Server initNewServer() {
        Server s = null;
        int port = Constants.DEFAULT_PORT;

        while (s == null) {
            try {
                s = initNewServer(port++);
            } catch (IOException ex) {
                log.log(Level.WARNING, "Error initializing server on port " + (port - 1), ex);
            }
        }

        return s;
    }
    private final ClientDB clients;
    private final ServerSocket serverSocket;
    private final HistoryManager history;
    private final ServerJobManagerImpl jobManager;
    private ClientDiscoveryDaemon cdd;

    private ServerImpl(final int port) throws IOException {
        history = new History();

        clients = new ClientDB();

        serverSocket = ServerSocket.createServerSocket(port, clients, clients);
        serverSocket.registerHistory(history);

        jobManager = new ServerJobManagerImpl(clients, serverSocket);
        getListenerRegistrator().setIdListener(Constants.ID_JOB_MANAGER, jobManager, true);

        getListenerRegistrator().setIdListener(Constants.ID_SYS_MSG, new SystemMessagesHandler(clients), true);

        if (ComponentSwitches.useClientDiscovery) {
            try {
                cdd = new ClientDiscoveryDaemon(serverSocket.getPort());
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

    @Override
    public Communicator registerClient(final InetAddress adress) throws ConnectionException {
        log.log(Level.INFO, "Registering new client on IP{0} on default port", adress);
        return clients.registerClient(adress, Constants.DEFAULT_PORT);
    }

    @Override
    public Communicator getClient(final InetAddress address) {
        Communicator result = clients.getClient(address, Constants.DEFAULT_PORT);

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

        if (cdd != null) {
            cdd.start();
        }
        jobManager.start();
    }

    @Override
    public void stopService() {
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
