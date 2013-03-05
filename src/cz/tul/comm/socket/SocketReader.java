package cz.tul.comm.socket;

import cz.tul.comm.socket.queue.IIdentifiable;
import cz.tul.comm.socket.queue.ObjectQueue;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SocketReader read data from socket and gives them to assigned handlers.
 *
 * @author Petr Ječmen
 */
public class SocketReader implements Runnable {

    private static final Logger log = Logger.getLogger(SocketReader.class.getName());
    private final Socket socket;
    private final ObjectQueue<IPData> dataStorageIP;
    private final ObjectQueue<IIdentifiable> dataStorageId;

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

    @Override
    public void run() {
        boolean dataReadAndHandled = false;

        try {
            final InetAddress ip = socket.getInetAddress();

            final ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            final Object o = in.readObject();
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
    }
}
