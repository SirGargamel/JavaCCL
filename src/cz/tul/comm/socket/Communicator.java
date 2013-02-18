package cz.tul.comm.socket;

import cz.tul.comm.client.Comm_Client;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Objects;

/**
 *
 * @author Petr Ječmen
 */
public final class Communicator {

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
            // TODO logging
            System.err.println(ex.getLocalizedMessage());
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
                result = responseHandler.pickupResponse(this);
            } catch (InterruptedException ex) {
                // TODO logging
                System.err.println(ex.getLocalizedMessage());
            }

        } catch (IOException ex) {
            // TODO logging
            System.err.println(ex.getLocalizedMessage());
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
