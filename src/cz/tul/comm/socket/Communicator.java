package cz.tul.comm.socket;

import cz.tul.comm.gui.UserLogging;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Objects;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class for communicating with registered client. Data are being sent using
 * sockets, so data class nedds to implement Serializable and all of its
 * children nedd to be Serializable as well (recursively).
 *
 * @see Serializable
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
        try (final Socket s = new Socket(address, port)) {
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
        responseHandler.registerResponse(address, this);
        if (sendData(data)) {
            try {

                synchronized (this) {
                    this.wait();
                }
                final Queue<Object> responses = responseHandler.getResponseQueue(this);
                result = responses.poll();
            } catch (InterruptedException ex) {
                log.log(Level.SEVERE, "Communicator wating for response has been interrupted.", ex);
            }
        }

        responseHandler.deregisterResponse(address, this);
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
        hash = 23 * hash + Objects.hashCode(this.address);
        hash = 23 * hash + this.port;
        return hash;
    }

}
