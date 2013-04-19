package cz.tul.comm.socket;

import cz.tul.comm.communicator.DataPacket;
import cz.tul.comm.history.IHistoryManager;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.messaging.MessageHeaders;
import cz.tul.comm.socket.queue.IIdentifiable;
import cz.tul.comm.socket.queue.ObjectQueue;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SocketReader read data from socket and gives them to assigned handlers.
 *
 * @author Petr Ječmen
 */
class SocketReader extends Observable implements Runnable {

    private static final Logger log = Logger.getLogger(SocketReader.class.getName());
    private final Socket socket;
    private final ObjectQueue<DataPacket> dataStorageClient;
    private final ObjectQueue<IIdentifiable> dataStorageId;
    private IHistoryManager hm;

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
            final ObjectQueue<DataPacket> dataStorageIP,
            final ObjectQueue<IIdentifiable> dataStorageId) {
        if (socket != null) {
            this.socket = socket;
        } else {
            throw new IllegalArgumentException("Socket cannot be null");
        }
        if (dataStorageIP != null) {
            this.dataStorageClient = dataStorageIP;
        } else {
            throw new IllegalArgumentException("Data storage cannot be null");
        }
        if (dataStorageId != null) {
            this.dataStorageId = dataStorageId;
        } else {
            throw new IllegalArgumentException("Data storage cannot be null");
        }
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
    public void run() {
        boolean dataRead = false;

        final InetAddress ip = socket.getInetAddress();
        Object o = null;
        try {
            final ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            o = in.readObject();
            dataRead = true;
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error reading data from socket.", ex);
        } catch (ClassNotFoundException ex) {
            log.log(Level.WARNING, "Invalid data received from sender.", ex);
        }

        try (final ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.writeBoolean(dataRead);
            out.flush();
            log.log(Level.FINE, "Reply sent.");
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error writing result data to socket.", ex);
        }

        if (hm != null) {
            hm.logMessageReceived(ip, o, dataRead);
        }

        if (o instanceof DataPacket) {
            final DataPacket dp = (DataPacket) o;
            dp.setSourceIP(ip);
            final Object data = dp.getData();            
            
            log.log(Level.CONFIG, "Data [{0}] received, storing to queues.", data.toString());

            setChanged();
            this.notifyObservers(dp);

            if (data instanceof IIdentifiable) {
                dataStorageId.storeData((IIdentifiable) data);
            }
            dataStorageClient.storeData(dp);                        
        } else if (o instanceof Message) { 
            final Message m = (Message) o;
            switch (m.getHeader()) {
                case (MessageHeaders.KEEP_ALIVE) :
                    // no further actions needed
                    break;
                default:
                    log.log(Level.WARNING, "Received Message with undefined header - {0}", new Object[]{m.toString()});
                    break;
            }
        } else {
            log.log(Level.WARNING, "Received data is not an instance of DataPacket - {0}", new Object[]{o});
        }
    }
}
