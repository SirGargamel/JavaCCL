package cz.tul.comm.socket;

import cz.tul.comm.socket.queue.IIdentifiable;
import cz.tul.comm.IService;
import cz.tul.comm.history.IHistoryManager;
import cz.tul.comm.socket.queue.IListener;
import cz.tul.comm.socket.queue.ObjectQueue;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Observer;
import java.util.Queue;
import java.util.Set;
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
public final class ServerSocket extends Thread implements IService, IListenerRegistrator {

    private static final Logger log = Logger.getLogger(ServerSocket.class.getName());
    private final java.net.ServerSocket socket;
    private final ExecutorService exec;
    private final ObjectQueue<IPData> dataStorageIP;
    private final ObjectQueue<IIdentifiable> dataStorageId;
    private final Set<Observer> dataListeners;
    private IHistoryManager hm;
    private int port;
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
        dataListeners = new HashSet<>();
        this.port = port;
        run = true;
    }

    @Override
    public Queue<IPData> addIpListener(final InetAddress address, final IListener dataListener, final boolean wantsPushNotifications) {
        log.log(Level.CONFIG, "Added new listener {0} for IP {1}", new Object[]{dataListener.toString(), address.getHostAddress()});
        return dataStorageIP.registerListener(address, dataListener, wantsPushNotifications);
    }

    @Override
    public void removeIpListener(InetAddress address, IListener dataListener) {
        if (address != null) {
            dataStorageIP.deregisterListener(address, dataListener);
            log.log(Level.CONFIG, "Removed listener {0} for IP {1}", new Object[]{dataListener.toString(), address.getHostAddress()});
        } else {
            log.log(Level.CONFIG, "Removed listener {0}", new Object[]{dataListener.toString()});
            dataStorageIP.deregisterListener(dataListener);
        }
    }

    @Override
    public Queue<IIdentifiable> addIdListener(final Object id, final IListener idListener, final boolean wantsPushNotifications) {
        log.log(Level.CONFIG, "Added new listener {0} for ID {1}", new Object[]{idListener.toString(), id.toString()});
        return dataStorageId.registerListener(id, idListener, wantsPushNotifications);
    }

    @Override
    public void removeIdListener(Object id, IListener idListener) {
        if (id != null) {
            log.log(Level.CONFIG, "Removed listener {0} for ID {1}", new Object[]{idListener.toString(), id.toString()});
            dataStorageId.deregisterListener(id, idListener);
        } else {
            log.log(Level.CONFIG, "Removed listener {0}", new Object[]{idListener.toString()});
            dataStorageId.deregisterListener(idListener);
        }
    }

    @Override
    public void registerMessageObserver(Observer msgObserver) {
        dataListeners.add(msgObserver);
        log.log(Level.CONFIG, "Added new message observer - {0}", new Object[]{msgObserver.toString()});
    }

    @Override
    public void deregisterMessageObserver(Observer msgObserver) {
        dataListeners.remove(msgObserver);
        log.log(Level.CONFIG, "Removed message observer - {0}", new Object[]{msgObserver.toString()});
    }

    @Override
    public void run() {
        Socket s;
        while (run) {
            try {
                s = socket.accept();
                log.log(Level.FINER, "Connection accepted from IP {0}:{1}", new Object[]{s.getInetAddress().getHostAddress(), s.getPort()});
                final SocketReader sr = new SocketReader(s, dataStorageIP, dataStorageId);
                sr.registerHistory(hm);
                for (Observer o : dataListeners) {
                    sr.addObserver(o);
                }
                exec.execute(sr);
            } catch (SocketException ex) {
                // nothing bad happened
                // required for proper shutdown
                log.config("Socket shutdown.");
            } catch (IOException ex) {
                log.log(Level.WARNING, "Server socket IO error occured during waiting for connection.", ex);
            }
        }
    }

    /**
     * @return listening port
     */
    public int getPort() {
        return port;
    }

    /**
     * Register history manager that will store info about received messages.
     *
     * @param hm instance of history manager
     */
    public void registerHistory(final IHistoryManager hm) {
        this.hm = hm;
        log.config("History registered.");
    }

    /**
     * Prepare new ServerSocket.
     *
     * @param port listening port
     * @return new instance of ServerSocket
     */
    public static ServerSocket createServerSocket(final int port) {
        ServerSocket result = new ServerSocket(port);
        result.start();
        log.finer("New server socket created and started.");
        return result;
    }

    @Override
    public void stopService() {
        try {
            run = false;
            socket.close();
            exec.shutdownNow();
            dataStorageIP.stopService();
            dataStorageId.stopService();
            log.config("Server socket has been stopped.");
        } catch (IOException ex) {
            log.log(Level.WARNING, "Server socket IO error occured during socket closing.", ex);
        }
    }
}
