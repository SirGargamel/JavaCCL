package cz.tul.comm.socket;

import cz.tul.comm.socket.queue.IIdentifiable;
import cz.tul.comm.IService;
import cz.tul.comm.socket.queue.IListener;
import cz.tul.comm.socket.queue.ObjectQueue;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Queue;
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
    private final ObjectQueue<IPData> dataStorageIP;
    private final ObjectQueue<IIdentifiable> dataStorageId;
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
        dataStorageIP = new ObjectQueue<>();
        dataStorageId = new ObjectQueue<>();
        run = true;
    }

    @Override
    public Queue<IPData> addIpListener(final InetAddress address, final IListener dataListener) {        
        return dataStorageIP.registerListener(address, dataListener);
    }

    @Override
    public void removeIpListener(InetAddress address, IListener dataListener) {
        dataStorageIP.deregisterListener(address, dataListener);
    }

    @Override
    public void removeIpListener(IListener dataListener) {
        dataStorageIP.deregisterListener(dataListener);
    }

    @Override
    public Queue<IIdentifiable> addIdListener(final Object id, final IListener idListener) {
        return dataStorageId.registerListener(id, idListener);
    }

    @Override
    public void removeIdListener(Object id, IListener idListener) {
        dataStorageId.deregisterListener(id, idListener);
    }

    @Override
    public void removeIdListener(IListener idListener) {
        dataStorageId.deregisterListener(idListener);
    }

    @Override
    public void run() {
        Socket s;
        while (run) {
            try {
                s = socket.accept();
                exec.execute(new SocketReader(s, dataStorageIP, dataStorageId));
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
