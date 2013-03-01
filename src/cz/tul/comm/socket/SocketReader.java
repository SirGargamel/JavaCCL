package cz.tul.comm.socket;

import cz.tul.comm.socket.queue.ObjectQueue;
import cz.tul.comm.socket.queue.IListener;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.messaging.MessageHeaders;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.UUID;
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
    private final ObjectQueue<UUID, IListener<UUID, Message>, Message> msgStorage;
    private final ObjectQueue<InetAddress, IListener<InetAddress, Object>, Object> dataStorage;

    public SocketReader(
            final Socket socket, 
            final ObjectQueue<UUID, IListener<UUID, Message>, Message> messageStorage, 
            final ObjectQueue<InetAddress, IListener<InetAddress, Object>, Object> dataStorage) {
        if (socket != null) {
            this.socket = socket;
        } else {
            throw new IllegalArgumentException("Socket cannot be null");
        }
        if (messageStorage != null) {
            this.msgStorage = messageStorage;
        } else {
            throw new IllegalArgumentException("Message storage cannot be null");
        }
        if (dataStorage != null) {
            this.dataStorage = dataStorage;
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
            if (o instanceof Message) {
                final Message m = (Message) o;

                if (m.getHeader().equals(MessageHeaders.SYSTEM)) {
                    System.err.println("SYS msg");
                    // TODO handle system message
                } else {
                    msgStorage.storeData(m.getId(), m);
                }
            } else {
                dataStorage.storeData(ip, o);
            }
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
