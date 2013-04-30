package cz.tul.comm.socket;

import cz.tul.comm.Constants;
import cz.tul.comm.GenericResponses;
import cz.tul.comm.communicator.DataPacket;
import cz.tul.comm.history.HistoryManager;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.messaging.MessageHeaders;
import cz.tul.comm.socket.queue.Identifiable;
import cz.tul.comm.socket.queue.Listener;
import cz.tul.comm.socket.queue.ObjectQueue;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;
import java.util.Observable;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SocketReader read data from socket and gives them to assigned handlers.
 *
 * @author Petr Ječmen
 */
class SocketReader extends Observable implements Runnable {

    private static final Logger log = Logger.getLogger(SocketReader.class.getName());
    private final ExecutorService exec;
    private final Socket socket;
    private final IDFilter idFilter;
    private final ObjectQueue<DataPacket> dataStorageClient;
    private final Map<UUID, Listener> clientListeners;
    private final ObjectQueue<Identifiable> dataStorageId;
    private final Map<Object, Listener> idListeners;
    private HistoryManager hm;

    /**
     * Create reader, which can read data from socket, tell sender outcome of
     * data reading and store data accoring to IP and ID.
     *
     * @param socket socket for reading
     * @param dataStorageIP IP listeners storage
     * @param dataStorageId ID listeners storage
     */
    SocketReader(
            final Socket socket,
            final IDFilter idFilter,
            final ObjectQueue<DataPacket> dataStorageIP,
            final Map<UUID, Listener> clientListeners,
            final ObjectQueue<Identifiable> dataStorageId,
            final Map<Object, Listener> idListeners) {
        if (socket != null) {
            this.socket = socket;
        } else {
            throw new NullPointerException("Socket cannot be null");
        }
        this.idFilter = idFilter;
        if (dataStorageIP != null) {
            this.dataStorageClient = dataStorageIP;
        } else {
            throw new IllegalArgumentException("Data storage cannot be null");
        }
        if (clientListeners != null) {
            this.clientListeners = clientListeners;
        } else {
            throw new NullPointerException("Client listeners cannot be null");
        }
        if (dataStorageId != null) {
            this.dataStorageId = dataStorageId;
        } else {
            throw new IllegalArgumentException("Data storage cannot be null");
        }
        if (idListeners != null) {
            this.idListeners = idListeners;
        } else {
            throw new NullPointerException("ID listeners cannot be null");
        }

        exec = Executors.newCachedThreadPool();
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
    public void run() {
        boolean dataReadV = false;
        final InetAddress ip = socket.getInetAddress();
        Object dataInV = null;
        try {
            final ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            dataInV = in.readObject();
            dataReadV = true;
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error reading data from socket.", ex);
        } catch (ClassNotFoundException ex) {
            log.log(Level.WARNING, "Invalid data received from sender.", ex);
        }
        final Object dataIn = dataInV;
        final boolean dataRead = dataReadV;

        if (dataIn instanceof DataPacket) {
            final DataPacket dp = (DataPacket) dataIn;
            dp.setSourceIP(ip);
            final UUID clientId = dp.getClientID();
            final Object data = dp.getData();

            if (idFilter != null) {
                if (clientId == null) {
                    log.log(Level.FINE, "Data with null id [{0}]", data.toString());
                } else if (!idFilter.isIdAllowed(clientId)) {
                    log.log(Level.WARNING, "Received data from unregistered client - id {0}, data [{1}]", new Object[]{clientId, data.toString()});
                    sendReply(ip, dataIn, dataRead, GenericResponses.UUID_NOT_ALLOWED);
                    return;
                }
            }

            log.log(Level.CONFIG, "Data [{0}] received, storing to queue.", data.toString());

            boolean handled = false;
            if (data instanceof Identifiable) {
                final Identifiable iData = (Identifiable) data;
                final Object id = iData.getId();
                if (id.equals(Constants.ID_SYS_MSG)) { // not pretty !!!
                    exec.submit(new Runnable() {
                        @Override
                        public void run() {
                            final Object response = idListeners.get(id).receiveData(dp);                            
                            sendReply(ip, dataIn, dataRead, response);                            
                        }
                    });
                    handled = true;
                } else if (idListeners.containsKey(id)) {
                    exec.submit(new Runnable() {
                        @Override
                        public void run() {
                            final Object response = idListeners.get(id).receiveData(iData);
                            sendReply(ip, dataIn, dataRead, response);
                        }
                    });
                    handled = true;
                } else {
                    dataStorageId.storeData((Identifiable) data);                    
                }
            } else {
                if (clientListeners.containsKey(clientId)) {
                    exec.submit(new Runnable() {
                        @Override
                        public void run() {
                            final Object response = clientListeners.get(clientId).receiveData(dp);
                            sendReply(ip, dataIn, dataRead, response);
                        }
                    });
                    handled = true;
                } else {
                    dataStorageClient.storeData(dp);                    
                }
            }
            if (!handled) {
                sendReply(ip, dataIn, dataRead, GenericResponses.NOT_HANDLED_DIRECTLY);
            }

            exec.submit(new Runnable() {
                @Override
                public void run() {
                    setChanged();
                    SocketReader.this.notifyObservers(dp);
                }
            });

        } else if (dataIn instanceof Message) {
            final Message m = (Message) dataIn;
            switch (m.getHeader()) {
                case (MessageHeaders.KEEP_ALIVE):
                    log.log(Level.FINE, "keepAlive received from {0}", ip.getHostAddress());
                    sendReply(ip, dataIn, dataRead, GenericResponses.OK);
                    break;
                default:
                    log.log(Level.WARNING, "Received Message with unidefined header - {0}", new Object[]{m.toString()});
                    sendReply(ip, dataIn, dataRead, GenericResponses.UNKNOWN_DATA);
                    break;
            }
        } else {
            log.log(Level.WARNING, "Received data is not an instance of DataPacket - {0}", new Object[]{dataIn});
            sendReply(ip, dataIn, dataRead, GenericResponses.ILLEGAL_DATA);
        }
    }

    private void sendReply(final InetAddress ip, Object dataIn, boolean dataRead, final Object response) {
        try (final ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.writeObject(response);
            out.flush();
            log.log(Level.FINE, "Reply sent.");
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error writing result data to socket.", ex);
        }

        if (hm != null) {
            hm.logMessageReceived(ip, dataIn, dataRead, response);
        }
    }
}
