package cz.tul.javaccl.socket;

import cz.tul.javaccl.Constants;
import cz.tul.javaccl.GenericResponses;
import cz.tul.javaccl.IService;
import cz.tul.javaccl.communicator.DataPacket;
import cz.tul.javaccl.history.HistoryManager;
import cz.tul.javaccl.messaging.Message;
import cz.tul.javaccl.messaging.SystemMessageHeaders;
import cz.tul.javaccl.messaging.Identifiable;
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
     * @param clientLister interface for obtaining list of registered clients
     * @return new instance of ServerSocket
     * @throws IOException error creating socket on given port
     */
    public static ServerSocket createServerSocket(final int port, final IDFilter idFilter, final ClientLister clientLister) throws IOException {
        ServerSocket result = new ServerSocket(port, idFilter, clientLister);
        result.start();
        log.fine("New server socket created and started.");
        return result;
    }
    private final java.net.ServerSocket socket;
    private final IDFilter idFilter;
    private final ExecutorService exec;
    private final Map<UUID, Listener<DataPacket>> listenersClient;
    private final Map<Object, Listener<Identifiable>> listenersId;
    private final ObjectQueue<DataPacket> dataStorageClient;
    private final ObjectQueue<Identifiable> dataStorageId;
    private final Set<Observer> dataListeners;
    private final MessagePullDaemon mpd;
    private HistoryManager hm;
    private boolean run;

    private ServerSocket(final int port, final IDFilter idFilter, final ClientLister clientLister) throws IOException {
        socket = new java.net.ServerSocket(port);
        this.idFilter = idFilter;
        exec = Executors.newCachedThreadPool();
        dataStorageClient = new ObjectQueue<>();
        dataStorageId = new ObjectQueue<>();
        dataListeners = new HashSet<>();
        run = true;
        listenersClient = new HashMap<>();
        listenersId = new HashMap<>();
        mpd = new MessagePullDaemon(this, clientLister);
    }

    @Override
    public void setClientListener(final UUID clientId, final Listener<DataPacket> dataListener) {
        log.log(Level.FINE, "Added new listener {0} for client with ID {1}", new Object[]{dataListener.toString(), clientId.toString()});
        listenersClient.put(clientId, dataListener);
    }

    @Override
    public Queue<DataPacket> createClientMessageQueue(final UUID clientId) {
        if (dataStorageClient.isListenerRegistered(clientId)) {
            return dataStorageClient.getDataQueue(clientId);
        } else {
            return dataStorageClient.prepareQueue(clientId);
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
    public void setIdListener(final Object id, final Listener<Identifiable> idListener) {
        log.log(Level.FINE, "Added new listener {0} for ID {1}", new Object[]{idListener.toString(), id.toString()});
        listenersId.put(id, idListener);
    }

    @Override
    public Queue<Identifiable> createIdMessageQueue(final Object id) {
        if (dataStorageId.isListenerRegistered(id)) {
            return dataStorageId.getDataQueue(id);
        } else {
            return dataStorageId.prepareQueue(id);
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
                final SocketReader sr = new SocketReader(s, this, mpd);
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
    public void start() {
        super.start();
        mpd.start();
    }

    @Override
    public void stopService() {
        try {
            run = false;
            mpd.stopService();
            exec.shutdownNow();
            socket.close();
            log.fine("Server socket has been stopped.");
        } catch (IOException ex) {
            // expected exception due to listening interruption
        }
    }

    @Override
    public Object handleDataPacket(final DataPacket dp) {
        final UUID clientId = dp.getSourceID();
        final Object data = dp.getData();
        Object result = GenericResponses.NOT_HANDLED;

        if (idFilter != null) {
            if (clientId == null) {
                boolean allowed = false;
                if (data instanceof Message) {
                    final UUID mId = ((Message) data).getId();
                    final String header = ((Message) data).getHeader();
                    if ((mId.equals(Constants.ID_SYS_MSG) && (header.equals(SystemMessageHeaders.LOGIN)) 
                            || header.equals(SystemMessageHeaders.STATUS_CHECK))) {
                        allowed = true;
                    }
                }
                if (!allowed) {
                    log.log(Level.FINE, "Data with null id received and its not a sys msg [{0}].", data.toString());
                    return GenericResponses.UUID_NOT_ALLOWED;
                }
            } else if (!idFilter.isTargetIdValid(dp.getTargetID())) {
                log.log(Level.FINE, "Received data not for this client - client id is {0}, data packet [{1}]", new Object[]{clientId, dp.toString()});
                return GenericResponses.ILLEGAL_TARGET_ID;
            } else if (!idFilter.isIdAllowed(clientId)) {
                log.log(Level.FINE, "Received data from unregistered client - id {0}, data [{1}]", new Object[]{clientId, data.toString()});
                return GenericResponses.UUID_NOT_ALLOWED;
            } else {
                log.log(Level.CONFIG, "Data [{0}] received, forwarding to listeners.", data.toString());
            }
        } else {
            log.log(Level.CONFIG, "Data [{0}] received, forwarding to listeners.", data.toString());
        }

        boolean sysMsg = false;
        if (data instanceof Identifiable) {
            final Identifiable iData = (Identifiable) data;
            final Object id = iData.getId();
            if (id.equals(Constants.ID_SYS_MSG)) { // not pretty !!!
                sysMsg = true;
                result = listenersId.get(id).receiveData(dp);
            } else if (listenersId.containsKey(id)) {
                result = listenersId.get(id).receiveData(iData);
            } else if (listenersClient.containsKey(clientId)) {
                result = listenersClient.get(clientId).receiveData(dp);
            } else if (dataStorageId.isListenerRegistered(id)) {
                dataStorageId.storeData(id, dp);
                result = GenericResponses.NOT_HANDLED_DIRECTLY;
            }
        }

        if (GenericResponses.NOT_HANDLED.equals(result)) {
            if (listenersClient.containsKey(clientId)) {
                result = listenersClient.get(clientId).receiveData(dp);
            } else if (dataStorageClient.isListenerRegistered(clientId)) {
                dataStorageClient.storeData(clientId, dp);
                result = GenericResponses.NOT_HANDLED_DIRECTLY;
            }
        }

        if (!sysMsg && !dataListeners.isEmpty()) {
            if (GenericResponses.NOT_HANDLED.equals(result)) {
                result = GenericResponses.NOT_HANDLED_DIRECTLY;
            }
            exec.submit(new Runnable() {
                @Override
                public void run() {
                    for (Observer o : dataListeners) {
                        o.update(null, dp);
                    }
                }
            });
        }

        return result;
    }
}