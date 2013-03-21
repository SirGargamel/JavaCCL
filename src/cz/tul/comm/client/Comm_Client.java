package cz.tul.comm.client;

import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.IService;
import cz.tul.comm.communicator.Status;
import cz.tul.comm.gui.UserLogging;
import cz.tul.comm.history.History;
import cz.tul.comm.history.IHistoryManager;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.messaging.MessageHeaders;
import cz.tul.comm.server.Comm_Server;
import cz.tul.comm.socket.IListenerRegistrator;
import cz.tul.comm.socket.ServerSocket;
import java.io.File;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class enclosing client to server communication. Allows data sending and has a
 * registry for message handling.
 *
 * @author Petr Ječmen
 */
public final class Comm_Client implements IService {

    private static final Logger log = Logger.getLogger(Comm_Client.class.getName());
    /**
     * Default port on which will client listen.
     */
    public static final int PORT = 5253;
    private final ServerSocket serverSocket;
    private final IHistoryManager history;
    private Communicator comm;
    private Status status;
    private ClientSystemMessaging csm;

    private Comm_Client(final int port) {
        history = new History();

        serverSocket = ServerSocket.createServerSocket(port);
        serverSocket.registerHistory(history);
        prepareServerCommunicator(InetAddress.getLoopbackAddress(), Comm_Server.PORT);        

        status = Status.ONLINE;
        csm = new ClientSystemMessaging(this);
        getListenerRegistrator().addIpListener(comm.getAddress(), csm, true);
        
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                ClientSettings.serialize(comm);
            }
        }));
    }        

    private void prepareServerCommunicator(final InetAddress address, final int port) {
        try {
            comm = new Communicator(address, port);
        } catch (IllegalArgumentException ex) {
            UserLogging.showErrorToUser(ex.getLocalizedMessage());
            log.log(Level.WARNING, "Illegal parameters for Communicator.", ex);
        }
    }

    /**
     *
     * @param address new IP of server
     */
    public void setServerAdress(final InetAddress address, final int port) {
        getListenerRegistrator().removeIpListener(null, csm);
        getListenerRegistrator().addIpListener(address, csm, true);
        prepareServerCommunicator(address, port);
    }

    /**
     * Send data to server.
     *
     * @param data data for sending
     */
    public void sendData(final Object data) {
        if (comm != null) {
            comm.sendData(data);
        }
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

    /**
     * @return interface for listener registration
     */
    public IListenerRegistrator getListenerRegistrator() {
        return serverSocket;
    }

    public Status getStatus() {
        return status;
    }

    /**
     * Create and initialize new instance of client.
     *
     * @return new Client instance
     */
    public static Comm_Client initNewClient(final int port) {
        final Comm_Client result = new Comm_Client(port);

        result.start();

        return result;
    }
    
    public static Comm_Client initNewClient() {
        return initNewClient(PORT);
    }

    private void start() {
        // load settings
        if (!ClientSettings.deserialize(this)) {
            // TODO tell user that settings are wrong
        }
        // registration
        final Message m = new Message(MessageHeaders.LOGIN, serverSocket.getPort());
        if (!comm.sendData(m)) {
            log.warning("Registeriing client to server failed.");
        }
    }

    @Override
    public void stopService() {
        serverSocket.stopService();
    }
}
