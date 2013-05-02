package cz.tul.comm.socket;

import cz.tul.comm.Constants;
import cz.tul.comm.GenericResponses;
import cz.tul.comm.IService;
import cz.tul.comm.communicator.DataPacket;
import cz.tul.comm.history.HistoryManager;
import cz.tul.comm.socket.queue.Identifiable;
import cz.tul.comm.socket.queue.Listener;
import cz.tul.comm.socket.queue.ObjectQueue;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
public class ServerSocket extends Thread implements IService, ListenerRegistrator, DataPacketHandler {

    private static final Logger log = Logger.getLogger(ServerSocket.class.getName());

    /**
     * Prepare new ServerSocket.
     *
     * @param port listening port
     * @param idFilter
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
    private final Map<UUID, Listener> listenersClient;
    private final Map<Object, Listener> listenersId;
    private final ObjectQueue<DataPacket> dataStorageClient;
    private final ObjectQueue<Identifiable> dataStorageId;
    private final Set<Observer> dataListeners;
    private HistoryManager hm;
    private boolean run;

    private ServerSocket(final int port, final IDFilter idFilter) throws IOException {
        socket = new java.net.ServerSocket(port);
        this.idFilter = idFilter;
        exec = Executors.newCachedThreadPool();
        dataStorageClient = new ObjectQueue<>();
        dataStorageId = new ObjectQueue<>();
        dataListeners = new HashSet<>();
        run = true;
        listenersClient = new HashMap<>();
        listenersId = new HashMap<>();
    }

    @Override
    public Queue<DataPacket> setClientListener(final UUID clientId, final Listener dataListener, final boolean wantsPushNotifications) {
        log.log(Level.FINE, "Added new listener {0} for ID {1}", new Object[]{dataListener.toString(), clientId});
        if (wantsPushNotifications) {
            listenersClient.put(clientId, dataListener);
            return null;
        } else {
            return dataStorageClient.setListener(clientId, dataListener);
        }

    }

    @Override
    public void removeClientListener(final UUID clientId) {
        if (clientId != null) {
            dataStorageClient.removeListener(clientId);
            listenersClient.remove(clientId);
            log.log(Level.FINE, "Removed listener for ID {1}", new Object[]{clientId});
        } else {
            log.log(Level.FINE, "NULL client id received for deregistration");
        }
    }

    @Override
    public Queue<Identifiable> setIdListener(final Object id, final Listener idListener, final boolean wantsPushNotifications) {
        log.log(Level.FINE, "Added new listener {0} for ID {1}", new Object[]{idListener.toString(), id.toString()});
        if (wantsPushNotifications) {
            listenersId.put(id, idListener);
            return null;
        } else {
            return dataStorageId.setListener(id, idListener);
        }
    }

    @Override
    public void removeIdListener(Object id) {
        if (id != null) {
            dataStorageId.removeListener(id);
            listenersId.remove(id);
            log.log(Level.FINE, "Removed listener for ID {0}", new Object[]{id.toString()});
        } else {
            log.log(Level.FINE, "NULL message id received for deregistration");
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
                final SocketReader sr = new SocketReader(s, this);
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
    public void registerHistory(final HistoryManager hm) {
        this.hm = hm;
        log.fine("History registered.");
    }

    @Override
    public void stopService() {
        try {
            run = false;
            exec.shutdownNow();
            socket.close();
            log.fine("Server socket has been stopped.");
        } catch (IOException ex) {
            // expected exception due to listening interruption
        }
    }

    @Override
    public Object handleDataPacket(final DataPacket dp) {
        final UUID clientId = dp.getClientID();
        final Object data = dp.getData();
        Object result = GenericResponses.NOT_HANDLED_DIRECTLY;

        if (idFilter != null) {
            if (clientId == null) {
                log.log(Level.FINE, "Data with null id [{0}]", data.toString());
            } else if (!idFilter.isIdAllowed(clientId)) {
                log.log(Level.WARNING, "Received data from unregistered client - id {0}, data [{1}]", new Object[]{clientId, data.toString()});
                return GenericResponses.UUID_NOT_ALLOWED;
            } else {
                log.log(Level.CONFIG, "Data [{0}] received, forwarding to listeners.", data.toString());
            }
        } else {
            log.log(Level.CONFIG, "Data [{0}] received, forwarding to listeners.", data.toString());
        }

        if (data instanceof Identifiable) {
            final Identifiable iData = (Identifiable) data;
            final Object id = iData.getId();
            if (id.equals(Constants.ID_SYS_MSG)) { // not pretty !!!
                result = listenersId.get(id).receiveData(dp);
            } else if (listenersId.containsKey(id)) {
                result = listenersId.get(id).receiveData(iData);
            } else {
                dataStorageId.storeData((Identifiable) data);
            }
        } else {
            if (listenersClient.containsKey(clientId)) {
                result = listenersClient.get(clientId).receiveData(dp);
            } else {
                dataStorageClient.storeData(dp);
            }
        }

        exec.submit(new Runnable() {
            @Override
            public void run() {
                for (Observer o : dataListeners) {
                    o.update(null, dp);
                }
            }
        });

        return result;
    }
}
