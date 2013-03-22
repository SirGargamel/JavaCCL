package cz.tul.comm.server;

import cz.tul.comm.persistence.ServerSettings;
import cz.tul.comm.IService;
import cz.tul.comm.client.Comm_Client;
import cz.tul.comm.history.History;
import cz.tul.comm.history.IHistoryManager;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.server.daemons.ClientDiscoveryDaemon;
import cz.tul.comm.server.daemons.ClientStatusDaemon;
import cz.tul.comm.socket.IListenerRegistrator;
import cz.tul.comm.socket.ServerSocket;
import java.io.File;
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
    /**
     * default port on which server will listen
     */
    public static final int PORT = 5252;
    private final ClientDB clients;
    private final ServerSocket serverSocket;
    private final IHistoryManager history;
    private final ClientStatusDaemon clientStatusDaemon;
    private ClientDiscoveryDaemon cdd;

    private Comm_Server(final int port) {
        history = new History();
        serverSocket = ServerSocket.createServerSocket(port);
        serverSocket.registerHistory(history);

        clients = new ClientDB();
        clientStatusDaemon = new ClientStatusDaemon(clients, serverSocket);

        getListenerRegistrator().registerMessageObserver(new SystemMessagesHandler(clients));
        try {
            cdd = new ClientDiscoveryDaemon(clients);
        } catch (SocketException ex) {
            log.log(Level.WARNING, "Failed to create ClientDiscoveryDaemon", ex);
        }

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
        return clients.registerClient(adress, Comm_Client.PORT);
    }

    public Communicator getClient(final InetAddress address) {
        return clients.getClient(address, Comm_Client.PORT);
    }

    /**
     * Export history as is to XML file.
     *
     * @param target target file
     * @return true for successfull export.
     */
    public boolean exportHistory(final File target) {
        return history.export(target, null);
    }

    /**
     * @return history manager for this client
     */
    public IHistoryManager getHistory() {
        return history;
    }

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

    /**
     * Create and initialize new instance of server.
     *
     * @return new instance of Comm_Server
     */
    public static Comm_Server initNewServer(final int port) {
        final Comm_Server result = new Comm_Server(port);

        result.start();

        return result;
    }

    public static Comm_Server initNewServer() {
        return initNewServer(PORT);
    }

    void start() {
        if (!ServerSettings.deserialize(clients)) {
            // TODO tell user loading settings has failed
        }
        clientStatusDaemon.start();
        if (cdd != null) {
            cdd.start();
        }
    }

    @Override
    public void stopService() {
        clientStatusDaemon.stopService();
        serverSocket.stopService();
        cdd.stopService();
    }
}
