package cz.tul.comm.socket;

import cz.tul.comm.IService;
import cz.tul.comm.communicator.DataPacket;
import cz.tul.comm.history.IHistoryManager;
import cz.tul.comm.socket.queue.IIdentifiable;
import cz.tul.comm.socket.queue.IListener;
import cz.tul.comm.socket.queue.ObjectQueue;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Observer;
import java.util.Queue;
import java.util.Set;
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

    /**
     * Prepare new ServerSocket.
     *
     * @param port listening port
     * @return new instance of ServerSocket
     * @throws IOException error creating socket on given port 
     */
    public static ServerSocket createServerSocket(final int port, final IDFilter idFilter) throws IOException {
        ServerSocket result = new ServerSocket(port, idFilter);
        result.start();
        log.fine("New server socket created and started.");
        return result;
    }
    private final java.net.ServerSocket socket;
    private final IDFilter idFilter;
    private final ExecutorService exec;
    private final ObjectQueue<DataPacket> dataStorageClient;
    private final ObjectQueue<IIdentifiable> dataStorageId;
    private final Set<Observer> dataListeners;    
    private IHistoryManager hm;
    private boolean run;

    private ServerSocket(final int port, final IDFilter idFilter) throws IOException {
        socket = new java.net.ServerSocket(port);
        this.idFilter = idFilter;
        exec = Executors.newCachedThreadPool();
        dataStorageClient = new ObjectQueue<>();
        dataStorageId = new ObjectQueue<>();
        dataListeners = new HashSet<>();        
        run = true;
    }

    @Override
    public Queue<DataPacket> addClientListener(final UUID clientId, final IListener dataListener, final boolean wantsPushNotifications) {
        log.log(Level.FINE, "Added new listener {0} for ID {1}", new Object[]{dataListener.toString(), clientId});
        return dataStorageClient.registerListener(clientId, dataListener, wantsPushNotifications);
    }

    @Override
    public void removeClientListener(final UUID clientId, final IListener dataListener) {
        if (clientId != null) {
            dataStorageClient.deregisterListener(clientId, dataListener);
            log.log(Level.FINE, "Removed listener {0} for ID {1}", new Object[]{dataListener.toString(), clientId});
        } else {
            log.log(Level.FINE, "Removed listener {0}", new Object[]{dataListener.toString()});
            dataStorageClient.deregisterListener(dataListener);
        }
    }

    @Override
    public Queue<IIdentifiable> addIdListener(final Object id, final IListener idListener, final boolean wantsPushNotifications) {
        log.log(Level.FINE, "Added new listener {0} for ID {1}", new Object[]{idListener.toString(), id.toString()});
        return dataStorageId.registerListener(id, idListener, wantsPushNotifications);
    }

    @Override
    public void removeIdListener(Object id, IListener idListener) {
        if (id != null) {
            log.log(Level.FINE, "Removed listener {0} for ID {1}", new Object[]{idListener.toString(), id.toString()});
            dataStorageId.deregisterListener(id, idListener);
        } else {
            log.log(Level.FINE, "Removed listener {0}", new Object[]{idListener.toString()});
            dataStorageId.deregisterListener(idListener);
        }
    }

    @Override
    public void addMessageObserver(Observer msgObserver) {
        dataListeners.add(msgObserver);
        log.log(Level.FINE, "Added new message observer - {0}", new Object[]{msgObserver.toString()});
    }

    @Override
    public void removeMessageObserver(Observer msgObserver) {
        dataListeners.remove(msgObserver);
        log.log(Level.FINE, "Removed message observer - {0}", new Object[]{msgObserver.toString()});
    }

    @Override
    public void run() {
        Socket s;
        while (run) {
            try {
                s = socket.accept();
                log.log(Level.FINE, "Connection accepted from IP {0}:{1}", new Object[]{s.getInetAddress().getHostAddress(), s.getPort()});
                final SocketReader sr = new SocketReader(s, idFilter, dataStorageClient, dataStorageId);
                sr.registerHistory(hm);
                for (Observer o : dataListeners) {
                    sr.addObserver(o);
                }
                exec.execute(sr);
            } catch (SocketException ex) {
                // nothing bad happened
                // required for proper shutdown                
            } catch (IOException ex) {
                log.log(Level.WARNING, "Server socket IO error occured during connection accepting.", ex);
            }
        }
    }

    /**
     * @return listening port
     */
    public int getPort() {
        return socket.getLocalPort();
    }

    /**
     * Register history manager that will store info about received messages.
     *
     * @param hm instance of history manager
     */
    public void registerHistory(final IHistoryManager hm) {
        this.hm = hm;
        log.fine("History registered.");
    }

    @Override
    public void stopService() {
        try {
            run = false;
            exec.shutdownNow();
            dataStorageClient.stopService();
            dataStorageId.stopService();
            socket.close();
            log.fine("Server socket has been stopped.");
        } catch (IOException ex) {
            // expected exception due to listening interruption
        }
    }
}
