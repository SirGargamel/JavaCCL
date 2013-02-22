package cz.tul.comm.socket;

import cz.tul.comm.gui.UserLogging;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class for communicating with registered client. Data are being sent using
 * sockets, so data class nedds to implement Serializable and all of its
 * children nedd to be Serializable as well (recursively). Client needs to
 * validate received data via sending true back.
 *
 * @see Serializable
 * @author Petr Ječmen
 */
public final class Communicator {

    private static final Logger log = Logger.getLogger(Communicator.class.getName());
    private final InetAddress address;
    private final int port;

    public Communicator(final InetAddress address, final int port) {
        this.address = address;
        this.port = port;
    }

    public InetAddress getAddress() {
        return address;
    }

    public boolean sendData(final Object data) {
        boolean result = false;
        try (final Socket s = new Socket(address, port)) {
            final ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
            out.writeObject(data);
            out.flush();
            try (final ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
                result = in.readBoolean();
            } catch (IOException ex) {
                log.log(Level.WARNING, "Error receiving response from output socket", ex);
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
        hash = 23 * hash + Objects.hashCode(this.address);
        hash = 23 * hash + this.port;
        return hash;
    }
}
