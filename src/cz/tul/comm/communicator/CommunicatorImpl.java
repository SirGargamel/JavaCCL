package cz.tul.comm.communicator;

import cz.tul.comm.GenericResponses;
import cz.tul.comm.history.HistoryManager;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.messaging.MessageHeaders;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Observable;
import java.util.Queue;
import java.util.UUID;
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
public class CommunicatorImpl extends Observable implements CommunicatorInner {

    private static final Logger log = Logger.getLogger(CommunicatorImpl.class.getName());

    /**
     * Create new communicator with given IP and on given port
     *
     * @param address target IP
     * @param port target port
     * @return created and initializaed instance of CommunicatorImpl
     */
    public static CommunicatorInner initNewCommunicator(final InetAddress address, final int port) {
        CommunicatorImpl c;
        try {
            c = new CommunicatorImpl(address, port);
            c.checkStatus();
        } catch (IllegalArgumentException ex) {
            log.log(Level.WARNING, "Illegal arguments used for Communicator creation.", ex);
            return null;
        }

        return c;
    }
    private final int TIMEOUT = 500;
    private final int STATUS_CHECK_INTERVAL = 5_000;
    private final InetAddress address;
    private final int port;
    private final Queue<DataPacket> unsentData;
    private final Map<DataPacket, Object> responses;
    private UUID id;
    private Calendar lastStatusUpdateTime;
    private Status status;
    private HistoryManager hm;

    private CommunicatorImpl(final InetAddress address, final int port) {
        if (address == null) {
            throw new IllegalArgumentException("Invalid address \"" + address + "\"");
        } else if (port < 0 || port > 65_535) {
            throw new IllegalArgumentException("Invalid port \"" + port + "\"");
        }

        this.address = address;
        this.port = port;

        unsentData = new LinkedList<>();
        responses = new HashMap<>();

        lastStatusUpdateTime = Calendar.getInstance();
        status = Status.NA;

        setChanged();
        notifyObservers();
    }

    /**
     * Register history manager that will store info about sent messages.
     *
     * @param hm instance of history manager
     */
    @Override
    public void registerHistory(final HistoryManager hm) {
        this.hm = hm;
        log.fine("History registered.");
    }

    @Override
    public InetAddress getAddress() {
        return address;
    }

    @Override
    public Object sendData(final Object data) {
        return sendData(data, 0);
    }

    @Override
    public Object sendData(final Object data, final int timeout) {
        boolean readAndReply = false;
        Object response = null;
        Status stat = Status.OFFLINE;
        DataPacket dp = new DataPacket(id, data);

        try (final Socket s = new Socket(address, port)) {
            s.setSoTimeout(timeout);

            final ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());

            out.writeObject(dp);
            out.flush();
            log.log(Level.CONFIG, "Data sent to {0}:{1} - [{2}]", new Object[]{getAddress().getHostAddress(), getPort(), data.toString()});

            try (final ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
                response = in.readObject();
                log.log(Level.FINE, "Received reply from client - {0}", response);
                stat = Status.ONLINE;
                readAndReply = true;
            } catch (IOException ex) {
                log.log(Level.WARNING, "Error receiving response from output socket", ex);
                stat = Status.NOT_RESPONDING;
            } catch (ClassNotFoundException ex) {
                log.log(Level.WARNING, "Unknown class object received.", ex);
                response = GenericResponses.UNKNOWN_DATA;
            }
        } catch (SocketTimeoutException ex) {
            log.log(Level.CONFIG, "Client on IP {0} is not responding to request.", address.getHostAddress());
            stat = Status.NOT_RESPONDING;
        } catch (IOException ex) {
            log.log(Level.WARNING, "Cannot write to output socket.", ex);
        }

        if (hm != null) {
            hm.logMessageSend(address, data, readAndReply, response);
        }
        if (!readAndReply) {
            unsentData.add(dp);
            try {
                response = waitForResponse(dp, timeout);
            } catch (InterruptedException ex) {
                log.log(Level.WARNING, "Waiting for response has been interrupted.", ex);
            }
        }

        setStatus(stat);
        return response;
    }

    private Object waitForResponse(DataPacket question, int timeout) throws InterruptedException {
        long startTime = Calendar.getInstance(Locale.getDefault()).getTimeInMillis();
        if (timeout > 0) {
            long dif = Calendar.getInstance(Locale.getDefault()).getTimeInMillis() - startTime;
            while (dif < timeout) {
                synchronized (this) {
                    this.wait(timeout);
                }
                dif = Calendar.getInstance(Locale.getDefault()).getTimeInMillis() - startTime;
            }
        } else {
            synchronized (this) {
                this.wait();
            }
        }
        if (responses.containsKey(question)) {
            return responses.get(question);
        } else {
            return GenericResponses.ERROR;
        }
    }

    @Override
    public Status checkStatus() {
        boolean result = false;
        final Object data = new Message(MessageHeaders.KEEP_ALIVE, null);
        Status stat = Status.OFFLINE;

        try (final Socket s = new Socket(address, port)) {
            s.setSoTimeout(TIMEOUT);

            final ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());

            stat = Status.NA;
            out.writeObject(data);
            out.flush();
            stat = Status.NOT_RESPONDING;

            try (final ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
                stat = Status.REACHABLE;
                in.readObject();
                stat = Status.ONLINE;
            } catch (IOException ex) {
                log.log(Level.FINE, "Client on IP {0} did not open stream for answer.", address.getHostAddress());
            } catch (ClassNotFoundException ex) {
                log.log(Level.WARNING, "Illegal class received from client for KEEP_ALIVE", ex);
            }
        } catch (SocketTimeoutException ex) {
            log.log(Level.FINE, "Client on IP {0} is not responding to request.", address.getHostAddress());
            stat = Status.NOT_RESPONDING;
        } catch (IOException ex) {
        }

        if (hm != null) {
            hm.logMessageSend(address, data, result, stat);
        }

        setStatus(stat);
        return getStatus();
    }

    public void setStatus(final Status newStatus) {
        status = newStatus;
        lastStatusUpdateTime = Calendar.getInstance();

        setChanged();
        notifyObservers(status);
    }

    /**
     * @return time of last status update
     */
    public Calendar getLastStatusUpdate() {
        return lastStatusUpdateTime;
    }

    @Override
    public Status getStatus() {
        if (Calendar.getInstance().getTimeInMillis() - getLastStatusUpdate().getTimeInMillis() > STATUS_CHECK_INTERVAL) {
            checkStatus();
        }

        return status;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public UUID getId() {
        return id;
    }

    /**
     * @param id new CommunicatorImpl UUID
     */
    @Override
    public void setId(UUID id) {
        this.id = id;
        setChanged();
        notifyObservers(this.id);
    }

    @Override
    public Queue<DataPacket> getUnsentData() {
        return unsentData;
    }

    @Override
    public void storeResponse(final DataPacket question, final Object response) {
        responses.put(question, response);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CommunicatorImpl) {
            CommunicatorImpl cc = (CommunicatorImpl) o;
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
