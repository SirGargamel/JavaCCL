package cz.tul.comm.socket;

import cz.tul.comm.IService;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.socket.queue.IListener;
import cz.tul.comm.socket.queue.ObjectQueue;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listening socket for data receiving. Listens for communication on given port
 * and when connection is made, server creates new {@link SocketReader} to
 * handle data receiving and starts listening again.
 *
 * @author Petr Ječmen
 */
public class ServerSocket extends Thread implements IService, IListenerRegistrator {

    private static final Logger log = Logger.getLogger(ServerSocket.class.getName());
    private final java.net.ServerSocket socket;
    private final ExecutorService exec;
    private final ObjectQueue<UUID, IListener<UUID, Message>, Message> msgStorage;
    private final ObjectQueue<InetAddress, IListener<InetAddress, Object>, Object> dataStorage;
    private boolean run;

    private ServerSocket(final int port) {
        java.net.ServerSocket s = null;
        try {
            s = new java.net.ServerSocket(port);
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Error creating socket on port " + port, ex);
        }
        socket = s;
        exec = Executors.newCachedThreadPool();
        msgStorage = new ObjectQueue<>();
        dataStorage = new ObjectQueue<>();
        run = true;
    }

    @Override
    public Queue<Object> addDataListener(final InetAddress address, final IListener<InetAddress, Object> dataListener) {
        return dataStorage.registerListener(address, dataListener);
    }

    @Override
    public void removeDataListener(InetAddress address, IListener<InetAddress, Object> dataListener) {
        dataStorage.deregisterListener(address, dataListener);
    }

    @Override
    public void removeDataListener(IListener<InetAddress, Object> dataListener) {
        dataStorage.deregisterListener(dataListener);
    }

    @Override
    public Queue<Message> addUUIDListener(final UUID id, final IListener<UUID, Message> idListener) {
        return msgStorage.registerListener(id, idListener);
    }

    @Override
    public void removeUUIDListener(UUID id, IListener<UUID, Message> idListener) {
        msgStorage.deregisterListener(id, idListener);
    }

    @Override
    public void removeUUIDListener(IListener<UUID, Message> idListener) {
        msgStorage.deregisterListener(idListener);
    }

    @Override
    public void run() {
        Socket s;
        while (run) {
            try {
                s = socket.accept();
                exec.execute(new SocketReader(s, msgStorage, dataStorage));
            } catch (SocketException ex) {
                // nothing bad happened
                // required for proper shutdown
                log.config("Socket shutdown.");
            } catch (IOException ex) {
                log.log(Level.WARNING, "Server socket IO error occured during waiting for connection.", ex);
            }
        }
        exec.shutdownNow();
    }

    public static ServerSocket createServerSocket(final int port) {
        ServerSocket result = new ServerSocket(port);
        result.start();
        return result;
    }

    @Override
    public void stopService() {
        try {
            socket.close();
        } catch (IOException ex) {
            log.log(Level.WARNING, "Server socket IO error occured during socket closing.", ex);
        }
        run = false;
    }
}
