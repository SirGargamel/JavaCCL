package cz.tul.comm.socket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Set;
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
    private final Set<IDataHandler> msgHandlers;

    public SocketReader(final Socket socket, final Set<IDataHandler> msgHandlers) {
        this.socket = socket;
        this.msgHandlers = msgHandlers;
    }

    @Override
    public void run() {
        boolean dataReadAndHandled = false;

        try {
            final ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            final Object o = in.readObject();
            for (IDataHandler mh : msgHandlers) {
                mh.handleData(socket.getInetAddress(), o);
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
