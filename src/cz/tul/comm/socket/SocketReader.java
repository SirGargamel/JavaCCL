package cz.tul.comm.socket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.Set;

/**
 *
 * @author Petr Ječmen
 */
public class SocketReader implements Runnable {

    private final Socket socket;
    private final Set<IMessageHandler> msgHandlers;

    public SocketReader(final Socket socket, final Set<IMessageHandler> msgHandlers) {
        this.socket = socket;
        this.msgHandlers = msgHandlers;
    }

    @Override
    public void run() {
        try {
            final ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            final Object o = in.readObject();
            for (IMessageHandler mh : msgHandlers) {
                mh.handleMessage(socket.getInetAddress(), o);
            }
        } catch (IOException | ClassNotFoundException ex) {
            // TODO logging
            System.err.println(ex.getLocalizedMessage());
        }
    }
}
