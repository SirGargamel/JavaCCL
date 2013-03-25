package cz.tul.comm.communicator;

import cz.tul.comm.history.IHistoryManager;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Date;
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
    private Date lastStatusUpdateTime;
    private Status status;
    private IHistoryManager hm;

    /**
     * New instance of communicator for sending data to given IP and port
     *
     * @param address target IP
     * @param port target port
     */
    public Communicator(final InetAddress address, final int port) {
        if (address == null) {
            throw new IllegalArgumentException("Invalid address \"" + address + "\"");
        } else if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port \"" + port + "\"");
        }

        this.address = address;
        this.port = port;

        lastStatusUpdateTime = new Date();
        status = Status.NA;
    }

    /**
     * Register history manager that will store info about sent messages.
     *
     * @param hm instance of history manager
     */
    public void registerHistory(final IHistoryManager hm) {
        this.hm = hm;
        log.config("History registered.");
    }

    /**
     *
     * @return target IP address
     */
    public InetAddress getAddress() {
        return address;
    }

    /**
     * Send data to given target.
     *
     * @param data data for sending (must implement Serializable interface)
     * @return true for successfull send
     */
    public boolean sendData(final Object data) {
        return sendData(data, 0);
    }

    /**
     * Send data to given target.
     *
     * @param data data for sending (must implement Serializable interface)
     * @param timeout time, after which sending is considered unsuccessfull
     * @return true for successfull send
     */
    public boolean sendData(final Object data, final int timeout) {
        boolean result = false;
        try (final Socket s = new Socket(address, port)) {
            s.setSoTimeout(timeout);

            final ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
            out.writeObject(data);
            out.flush();
            log.log(Level.FINER, "Data sent to client {0}:{1}", new Object[]{getAddress().getHostAddress(), getPort()});
            try (final ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
                result = in.readBoolean();
                log.log(Level.FINER, "Received reply from client - {0}", result);
            } catch (IOException ex) {
                log.log(Level.WARNING, "Error receiving response from output socket", ex);
                setStatus(Status.NOT_RESPONDING);
            }
        } catch (SocketTimeoutException ex) {
            log.log(Level.FINER, "Client on IP {0} is not responding to request.", address.getHostAddress());
            setStatus(Status.NOT_RESPONDING);
        } catch (IOException ex) {
            log.log(Level.WARNING, "Cannot write to output socket.", ex);
            setStatus(Status.OFFLINE);
        }

        if (hm != null) {
            hm.logMessageSend(address, data, result);
        }

        return result;
    }

    /**
     * @param newStatus new client status
     */
    public void setStatus(final Status newStatus) {
        status = newStatus;
        lastStatusUpdateTime = new Date();
    }

    /**
     *
     * @return time of last status update
     */
    public Date getLastStatusUpdate() {
        return lastStatusUpdateTime;
    }

    /**
     * @return last known client status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * @return target client port
     */
    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Communicator) {
            Communicator cc = (Communicator) o;
            return (cc.getAddress().equals(address) && cc.getPort() == port);
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
