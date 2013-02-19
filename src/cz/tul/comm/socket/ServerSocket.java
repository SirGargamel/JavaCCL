package cz.tul.comm.socket;

import cz.tul.comm.IService;
import java.io.IOException;
import java.net.BindException;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listening socket for data receiving. Listens for communication on given port
 * and when connection is made, server creates new {@link SocketReader} to
 * handle data receiving and starts listening again.
 * @author Petr Ječmen
 */
public class ServerSocket extends Thread implements IService {

    private static final Logger log = Logger.getLogger(ServerSocket.class.getName());
    private final java.net.ServerSocket socket;
    private final ExecutorService exec;
    private final Set<IMessageHandler> msgHandlers;
    private boolean run;

    public ServerSocket(final int port) throws BindException {
        try {
            socket = new java.net.ServerSocket(port);
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Error creating socket on port " + port, ex);
            throw new BindException("Port number - " + port);
        }
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
                log.config("Socket shutdown.");
            } catch (IOException ex) {
                log.log(Level.WARNING, "Server socket IO error occured during waiting for connection.", ex);
            }
        }
        exec.shutdownNow();
    }

    @Override
    public void stopService() {
        try {
            socket.close();
        } catch (IOException ex) {
            log.log(Level.WARNING, "Server socket IO error occured during socket closing.", ex);
        }
        run = false;
    }
}
