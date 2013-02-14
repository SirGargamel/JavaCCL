package cz.tul.comm.client;

import cz.tul.comm.socket.Communicator;
import cz.tul.comm.socket.IMessageHandler;
import cz.tul.comm.IService;
import cz.tul.comm.server.Comm_Server;
import cz.tul.comm.socket.ServerSocket;
import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class Comm_Client implements IService {

    public static final int PORT = 5253;
    private final ServerSocket serverSocket;
    private final Communicator comm;
    private InetAddress serverAdress;

    private Comm_Client() {
        ServerSocket tmp = null;
        try {
            tmp = new ServerSocket(PORT);
        } catch (IOException ex) {
            // TODO logging
            Logger.getLogger(Comm_Server.class.getName()).log(Level.SEVERE, null, ex);
            // TODO handle that socket cannot be bound
        }
        serverSocket = tmp;

        // TODO server IP settings
        serverAdress = InetAddress.getLoopbackAddress();
        comm = new Communicator(serverAdress, Comm_Server.PORT);
    }

    public void addMessageHandler(final IMessageHandler msgHandler) {
        serverSocket.addMessageHandler(msgHandler);
    }

    public void sendMessage(final Object data) {
        comm.sendData(data);
    }

    void start() {
        serverSocket.start();
    }

    public static Comm_Client initNewClient() {
        final Comm_Client result = new Comm_Client();

        result.start();

        return result;
    }

    @Override
    public void stopService() {
        serverSocket.stopService();
    }
}
