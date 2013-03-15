package cz.tul.comm.socket;

import cz.tul.comm.history.IHistoryManager;
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
    private final ObjectQueue<IPData> dataStorageIP;
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
    public SocketReader(
            final Socket socket,
            final ObjectQueue<IPData> dataStorageIP,
            final ObjectQueue<IIdentifiable> dataStorageId) {
        if (socket != null) {
            this.socket = socket;
        } else {
            throw new IllegalArgumentException("Socket cannot be null");
        }
        if (dataStorageIP != null) {
            this.dataStorageIP = dataStorageIP;
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
    }

    @Override
    public void run() {
        boolean dataReadAndHandled = false;

        final InetAddress ip = socket.getInetAddress();
        Object o = null;
        try {
            final ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            o = in.readObject();

            setChanged();
            this.notifyObservers(o);

            if (o instanceof IIdentifiable) {
                dataStorageId.storeData((IIdentifiable) o);
            }
            dataStorageIP.storeData(new IPData(ip, o));
            dataReadAndHandled = true;
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error reading data from socket.", ex);
        } catch (ClassNotFoundException ex) {
            log.log(Level.WARNING, "Invalid data received from sender.", ex);
        }

        try (final ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.writeBoolean(dataReadAndHandled);
            out.flush();
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error writing result data to socket.", ex);
        }

        if (hm != null) {
            hm.logMessageReceived(ip, o, dataReadAndHandled);
        }
    }
}
