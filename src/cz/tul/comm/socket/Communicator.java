package cz.tul.comm.socket;

import cz.tul.comm.client.Comm_Client;
import cz.tul.comm.gui.UserLogging;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Objects;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Ječmen
 */
public final class Communicator {

    private static final Logger log = Logger.getLogger(Communicator.class.getName());
    private final InetAddress address;
    private final int port;
    private IResponseHandler responseHandler;

    public Communicator(final InetAddress address, final int port) {
        this.address = address;
        this.port = port;
    }

    public void registerMessageHandler(final IResponseHandler handler) {
        this.responseHandler = handler;
    }

    public InetAddress getAddress() {
        return address;
    }

    public boolean sendData(final Object data) {
        try (Socket s = new Socket(address, port)) {
            final ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
            out.writeObject(data);
            return true;
        } catch (IOException ex) {
            log.log(Level.WARNING, "Cannot write to output socket", ex);
            UserLogging.showWarningToUser("Could not contact client with IP " + address.getHostAddress());
            return false;
        }
    }

    public Object sendAndReceiveData(final Object data) {
        if (responseHandler == null) {
            throw new IllegalStateException("Message handler not assigned.");
        }

        Object result = null;
        try (Socket s = new Socket(address, Comm_Client.PORT)) {
            final ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
            out.writeObject(data);
            try {
                responseHandler.registerResponse(address, this);

                synchronized (this) {
                    this.wait();
                }
                Queue<Object> responses = responseHandler.getResponseQueue(this);

                result = responses.poll();

                responseHandler.deregisterResponse(address, this);
            } catch (InterruptedException ex) {
                log.log(Level.SEVERE, "Communicator wating for response has been interrupted.", ex);
            }
        } catch (IOException ex) {
            log.log(Level.WARNING, "Cannot write to output socket", ex);
            UserLogging.showWarningToUser("Could not contact client with IP " + address.getHostAddress());
        }

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Communicator) {
            Communicator cc = (Communicator) o;
            if (cc.getAddress().equals(address)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 37 * hash + Objects.hashCode(this.address);
        return hash;
    }
}
