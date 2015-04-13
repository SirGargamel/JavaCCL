package cz.tul.javaccl.socket;

import cz.tul.javaccl.GlobalConstants;
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
public final class ServerSocket extends Thread implements IService, ListenerRegistrator, DataPacketHandler {

    private static final Logger LOG = Logger.getLogger(ServerSocket.class.getName());

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
        LOG.fine("New server socket created and started.");
        return result;
    }
    private final java.net.ServerSocket socket;
    private IDFilter idFilter;
    private final ExecutorService exec;
    private final Map<UUID, Listener<DataPacket>> listenersClient;
    private final Map<Object, Listener<Identifiable>> listenersId;
    private Listener<DataPacket> messageListener;
    private final ObjectQueue<DataPacket> dataStorageClient;
    private final ObjectQueue<Identifiable> dataStorageId;
    private final Set<Observer> dataListeners;
    private final MessagePullDaemon mpd;
    private HistoryManager hManager;
    private boolean run;

    private ServerSocket(final int port, final IDFilter idFilter, final ClientLister clientLister) throws IOException {
        super();

        socket = new java.net.ServerSocket(port);
        this.idFilter = idFilter;
        exec = Executors.newCachedThreadPool();
        dataStorageClient = new ObjectQueue<DataPacket>();
        dataStorageId = new ObjectQueue<Identifiable>();
        dataListeners = new HashSet<Observer>();
        run = true;
        listenersClient = new HashMap<UUID, Listener<DataPacket>>();
        listenersId = new HashMap<Object, Listener<Identifiable>>();
        mpd = new MessagePullDaemon(this, clientLister);
    }

    @Override
    public void setClientListener(final UUID clientId, final Listener<DataPacket> dataListener) {
        LOG.log(Level.FINE, "Added new listener " + dataListener.toString() + " for client with ID " + clientId.toString());
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
            LOG.log(Level.FINE, "Removed listener for ID " + clientId);
        } else {
            LOG.log(Level.FINE, "NULL client id received for deregistration");
        }
    }

    @Override
    public void setIdListener(final Object msgId, final Listener<Identifiable> idListener) {
        LOG.log(Level.FINE, "Added new listener " + idListener.toString() + " for ID " + msgId.toString());
        listenersId.put(msgId, idListener);
    }

    @Override
    public Queue<Identifiable> createIdMessageQueue(final Object msgId) {
        if (dataStorageId.isListenerRegistered(msgId)) {
            return dataStorageId.getDataQueue(msgId);
        } else {
            return dataStorageId.prepareQueue(msgId);
        }
    }

    @Override
    public void removeIdListener(final Object msgId) {
        if (msgId != null) {
            dataStorageId.removeListener(msgId);
            listenersId.remove(msgId);
            LOG.log(Level.FINE, "Removed listener for ID " + msgId.toString());
        } else {
            LOG.log(Level.FINE, "NULL message id received for deregistration");
        }
    }

    @Override
    public void addMessageObserver(final Observer msgObserver) {
        dataListeners.add(msgObserver);
        LOG.log(Level.FINE, "Added new message observer - " + msgObserver.toString());
    }

    @Override
    public void removeMessageObserver(final Observer msgObserver) {
        dataListeners.remove(msgObserver);
        LOG.log(Level.FINE, "Removed message observer - " + msgObserver.toString());
    }

    @Override
    public void setMessageListener(final Listener<DataPacket> listener) {
        this.messageListener = listener;
        LOG.log(Level.FINE, "Set new message listener - " + listener.toString());
    }

    @Override
    public void removeMessageListener() {
        LOG.log(Level.FINE, "Removed message listener - " + messageListener.toString());
        messageListener = null;
    }

    @Override
    public Queue<DataPacket> getMessageQueue() {
        final UUID localId = idFilter.getLocalID();
        if (dataStorageClient.isListenerRegistered(localId)) {
            return dataStorageClient.getDataQueue(localId);
        } else {
            return dataStorageClient.prepareQueue(localId);
        }
    }

    @Override
    public void run() {
        Socket s;
        while (run) {
            try {
                s = socket.accept();
                LOG.log(Level.FINE, "Connection accepted from IP " + s.getInetAddress().getHostAddress() + ":" + s.getPort());
                final SocketReader sr = new SocketReader(s, this, mpd);
                sr.registerHistory(hManager);
                for (Observer o : dataListeners) {
                    sr.addObserver(o);
                }
                exec.execute(sr);
            } catch (SocketException ex) {
                // nothing bad happened
                // required for proper shutdown                
            } catch (IOException ex) {
                LOG.log(Level.WARNING, "Server socket IO error occured during connection accepting.");
                LOG.log(Level.FINE, "Server socket IO error occured during connection accepting.", ex);
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
     * @param manager instance of history manager
     */
    public void registerHistory(final HistoryManager manager) {
        this.hManager = manager;
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
        } catch (IOException ex) {
            // expected exception due to listening interruption
        }
        LOG.fine("Server socket has been stopped.");
    }

    @Override
    public Object handleDataPacket(final DataPacket dp) {
        final UUID clientId = dp.getSourceId();
        final Object data = dp.getData();
        Object result = GenericResponses.NOT_HANDLED;

        boolean allowed = false;
        if (idFilter == null) {
            LOG.log(Level.FINE, "Data [{0}] received, no ID filter set, forwarding to listeners.", data.toString());
            allowed = true;
        } else {
            if (!idFilter.isTargetIdValid(dp.getTargetId())) {
                if (data instanceof Message) {
                    final UUID mId = ((Message) data).getId();
                    final String header = ((Message) data).getHeader();
                    if (mId.equals(GlobalConstants.ID_SYS_MSG) && header.equals(SystemMessageHeaders.LOGIN)) {
                        allowed = true;
                    }
                }

                if (!allowed) {
                    LOG.log(Level.FINE, "Received data not for this client - client id {0}, data packet [{1}]", new Object[]{clientId, dp.toString()});
                    result = GenericResponses.ILLEGAL_TARGET_ID;
                }
            } else if (!idFilter.isIdAllowed(clientId)) {
                LOG.log(Level.FINE, "Received data from unregistered client - id {0}, data [{1}]", new Object[]{clientId, data.toString()});
                result = GenericResponses.UUID_NOT_ALLOWED;
            } else {
                LOG.log(Level.FINE, "Data [{0}] received, forwarding to listeners.", data.toString());
                allowed = true;
            }
        }

        if (allowed) {
            boolean sysMsg = false;
            boolean handled;
            if (data instanceof Identifiable) {
                final Identifiable iData = (Identifiable) data;
                final Object id = iData.getId();
                if (id.equals(GlobalConstants.ID_SYS_MSG)) { // not pretty !!!
                    sysMsg = true;
                    result = listenersId.get(id).receiveData(dp);
                    handled = true;
                } else if (listenersId.containsKey(id)) {
                    result = listenersId.get(id).receiveData(iData);
                    handled = true;
                } else if (listenersClient.containsKey(clientId)) {
                    result = listenersClient.get(clientId).receiveData(dp);
                    handled = true;
                } else {
                    handled = false;
                }
                if (dataStorageId.isListenerRegistered(id)) {
                    dataStorageId.storeData(id, dp);
                    if (!handled) {
                        result = GenericResponses.NOT_HANDLED_DIRECTLY;
                    }
                }
            } else {
                handled = false;
            }

            if (!sysMsg) {
                if (!handled && listenersClient.containsKey(clientId)) {
                    result = listenersClient.get(clientId).receiveData(dp);
                    handled = true;
                }
                if (!handled && messageListener != null) {
                    result = messageListener.receiveData(dp);
                    handled = true;
                }

                if (dataStorageClient.isListenerRegistered(clientId)) {
                    dataStorageClient.storeData(clientId, dp);
                    if (!handled) {
                        result = GenericResponses.NOT_HANDLED_DIRECTLY;
                    }
                }
                if (dataStorageClient.isListenerRegistered(idFilter.getLocalID())) {
                    dataStorageClient.storeData(idFilter.getLocalID(), dp);
                    if (!handled) {
                        result = GenericResponses.NOT_HANDLED_DIRECTLY;
                    }
                }
                if (!dataListeners.isEmpty()) {
                    if (!handled) {
                        result = GenericResponses.NOT_HANDLED_DIRECTLY;
                    }
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            for (Observer o : dataListeners) {
                                o.update(null, dp);
                            }
                        }
                    });
                }
            }
        }

        return result;
    }

    public void setIdFilter(final IDFilter idFilter) {
        this.idFilter = idFilter;
    }
}
