package cz.tul.comm.socket;

import cz.tul.comm.IService;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class ServerSocket extends Thread implements IService {

    private final java.net.ServerSocket socket;
    private final ExecutorService exec;
    private final Set<IMessageHandler> msgHandlers;
    private boolean run;

    public ServerSocket(final int port) throws IOException {
        socket = new java.net.ServerSocket(port);
        exec = Executors.newCachedThreadPool();
        msgHandlers = new HashSet<>(1);
        run = true;
    }

    public void addMessageHandler(final IMessageHandler handler) {
        msgHandlers.add(handler);
    }

    @Override
    public void run() {
        Socket s;
        while (run) {
            try {
                s = socket.accept();
                exec.execute(new SocketReader(s, msgHandlers));
            } catch (SocketException ex) {
                // nothing bad happened
                // required for proper shutdown
            } catch (IOException ex) {
                // TODO logging
                Logger.getLogger(ServerSocket.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        exec.shutdownNow();
    }

    @Override
    public void stopService() {
        try {
            socket.close();
        } catch (IOException ex) {
            // TODO logging
            System.err.println("IO Exception during socket closing.\n" + ex.getLocalizedMessage());
        }
        run = false;
    }
}
