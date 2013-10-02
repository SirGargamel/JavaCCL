package cz.tul.javaccl.communicator;

import cz.tul.javaccl.Constants;
import cz.tul.javaccl.GenericResponses;
import cz.tul.javaccl.Utils;
import cz.tul.javaccl.exceptions.ConnectionException;
import cz.tul.javaccl.exceptions.ConnectionExceptionCause;
import cz.tul.javaccl.history.HistoryManager;
import cz.tul.javaccl.messaging.Message;
import cz.tul.javaccl.messaging.SystemMessageHeaders;
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
    private static final Object dummy = "dummyObject";

    /**
     * Create new communicator with given IP and on given port
     *
     * @param address target IP
     * @param port target port
     * @return created and initializaed instance of CommunicatorImpl
     */
    public static CommunicatorInner initNewCommunicator(final InetAddress address, final int port) {
        return new CommunicatorImpl(address, port);
    }    
    private final int MSG_PULL_TIME_LIMIT = 2000;
    private final int STATUS_CHECK_TIMEOUT = 200;
    private final int STATUS_CHECK_INTERVAL = 500;
    private final int REPONSE_CHECK_INTERVAL = 100;
    private final InetAddress address;
    private final int port;
    private final Queue<DataPacket> unsentData;
    private final Map<DataPacket, Object> responses;
    private UUID sourceId;
    private UUID targetId;
    private Calendar lastStatusUpdateTime;
    private Calendar lastMsgPull;
    private Status status;
    private HistoryManager hm;

    private CommunicatorImpl(final InetAddress address, final int port) throws IllegalArgumentException {
        if (address == null) {
            throw new IllegalArgumentException("Invalid address \"" + address + "\"");
        } else if (port < 0 || port > 65_535) {
            throw new IllegalArgumentException("Invalid port \"" + port + "\"");
        }

        this.address = address;
        this.port = port;

        unsentData = new LinkedList<>();
        responses = new HashMap<>();

        status = Status.OFFLINE;

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
    public Object sendData(final Object data) throws IllegalArgumentException, ConnectionException {
        return sendData(data, Constants.DEFAULT_TIMEOUT);
    }

    @Override
    public Object sendData(final Object data, final int timeout) throws IllegalArgumentException, ConnectionException {
        if (!Utils.checkSerialization(data)) {
            throw new IllegalArgumentException("Data for sending (and all of its members) must be serializable (eg. implement Serializable or Externalizable interface.)");
        }

        boolean readAndReply = false;
        Object response = dummy;
        Status stat = getStatus();
        DataPacket dp = new DataPacketImpl(sourceId, targetId, data);

        if (stat.equals(Status.ONLINE)) {
            response = pushDataToOnlineClient(dp, timeout);
            if (response == GenericResponses.ILLEGAL_TARGET_ID) {
                throw new ConnectionException(ConnectionExceptionCause.WRONG_TARGET);
            } else if (response == GenericResponses.UUID_NOT_ALLOWED) {
                throw new ConnectionException(ConnectionExceptionCause.UUID_NOT_ALLOWED);
            } else if (response == GenericResponses.CONNECTION_ERROR) {
                throw new ConnectionException(ConnectionExceptionCause.CONNECTION_ERROR);
            } else {
                readAndReply = true;
            }
        } else if (stat.equals(Status.PASSIVE)) {
            if (!readAndReply && !unsentData.contains(dp)) {
                unsentData.add(dp);
                try {
                    response = waitForResponse(dp, timeout);
                    readAndReply = true;
                } catch (InterruptedException ex) {
                    log.log(Level.WARNING, "Waiting for response has been interrupted.", ex);
                }
            }
        } else {
            throw new ConnectionException(ConnectionExceptionCause.CONNECTION_ERROR);
        }

        if (hm != null) {
            hm.logMessageSend(address, data, readAndReply, response);
        }

        return response;
    }

    private Object pushDataToOnlineClient(final DataPacket dp, final int timeout) throws ConnectionException {
        Object response = dummy;
        try (final Socket s = new Socket(address, port)) {
            s.setSoTimeout(timeout);

            final ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());

            out.writeObject(dp);
            out.flush();

            if (dp.getData() != null) {
                log.log(Level.CONFIG, "Data sent to client with ID {0} - [{1}]", new Object[]{getTargetId(), dp.getData().toString()});
            } else {
                log.log(Level.CONFIG, "NULL data sent to client with ID {0} - [{1}]", new Object[]{getTargetId()});
            }

            try (final ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
                response = in.readObject();
                log.log(Level.FINE, "Received reply from client - {0}", response);
            } catch (IOException ex) {
                log.log(Level.WARNING, "Error receiving response from output socket", ex);
            } catch (ClassNotFoundException ex) {
                log.log(Level.WARNING, "Unknown class object received.", ex);
                response = GenericResponses.ILLEGAL_DATA;
            }
        } catch (SocketTimeoutException ex) {
            throw new ConnectionException(ConnectionExceptionCause.TIMEOUT);
        } catch (IOException ex) {
            log.log(Level.WARNING, "Cannot write to output socket.", ex);
        }
        return response;
    }

    private Object waitForResponse(final DataPacket question, final int timeout) throws InterruptedException, ConnectionException {
        Status stat;
        final int time;
        if (timeout > 0) {
            time = timeout;
        } else {
            time = Integer.MAX_VALUE;
        }

        final long startTime = Calendar.getInstance(Locale.getDefault()).getTimeInMillis();
        long dif = Calendar.getInstance(Locale.getDefault()).getTimeInMillis() - startTime;
        while (!responses.containsKey(question) && (dif <= time)) {
            synchronized (this) {
                this.wait(REPONSE_CHECK_INTERVAL);
            }
            stat = checkStatus();
            if (stat.equals(Status.ONLINE)) {
                try {
                    Object response = pushDataToOnlineClient(question, timeout);
                    if (response != dummy) {
                        unsentData.remove(question);
                        responses.put(question, response);
                    }
                } catch (ConnectionException ex) {
                    log.warning("Online client conneciton timed out.");
                }

            }
            dif = Calendar.getInstance(Locale.getDefault()).getTimeInMillis() - startTime;
        }

        if (unsentData.contains(question)) {
            unsentData.remove(question);
        }

        if (responses.containsKey(question)) {
            return responses.get(question);
        } else {
            throw new ConnectionException(ConnectionExceptionCause.TIMEOUT);
        }
    }

    @Override
    public Status checkStatus() {
        boolean result = false;
        final Object data = new Message(Constants.ID_SYS_MSG, SystemMessageHeaders.STATUS_CHECK, null);
        final DataPacket dp = new DataPacketImpl(sourceId, targetId, data);
        Status stat = Status.OFFLINE;

        try (final Socket s = new Socket(address, port)) {
            s.setSoTimeout(STATUS_CHECK_TIMEOUT);

            final ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());

            out.writeObject(dp);
            out.flush();

            try (final ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
                Object response = in.readObject();
                if ((targetId != null && targetId.equals(response))
                        || targetId == null && response == null) {
                    stat = Status.ONLINE;
                } else {
                    log.log(Level.FINE, "STATUS_CHECK response received for another ID. {0} , {1}", new Object[]{response, targetId});
                }
            } catch (IOException ex) {
                log.log(Level.FINE, "Client on IP {0} did not open stream for answer.", address.getHostAddress());
            } catch (ClassNotFoundException ex) {
                log.log(Level.WARNING, "Illegal class received from client for KEEP_ALIVE", ex);
            }
        } catch (SocketTimeoutException ex) {
            log.log(Level.FINE, "Client on IP {0} is not responding to request.", address.getHostAddress());
        } catch (IOException ex) {
        }

        if (hm != null) {
            hm.logMessageSend(address, data, result, stat);
        }

        if (stat.equals(Status.OFFLINE) && lastMsgPull != null) {
            final Calendar currentTime = Calendar.getInstance();
            final long dif = currentTime.getTimeInMillis() - lastMsgPull.getTimeInMillis();
            if (dif < MSG_PULL_TIME_LIMIT) {
                stat = Status.PASSIVE;
            }
        }

        setStatus(stat);
        return stat;
    }

    private void setStatus(final Status newStatus) {
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
    public boolean isOnline() {
        final Status stat = getStatus();
        return (stat.equals(Status.ONLINE) || stat.equals(Status.PASSIVE));
    }

    @Override
    public Status getStatus() {
        Calendar lastUpdate = getLastStatusUpdate();
        if (lastUpdate == null
                || (Calendar.getInstance().getTimeInMillis() - lastUpdate.getTimeInMillis() > STATUS_CHECK_INTERVAL)) {
            checkStatus();
        }

        return status;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public UUID getTargetId() {
        return targetId;
    }

    @Override
    public void setTargetId(UUID id) {
        this.targetId = id;
        setChanged();
        notifyObservers(this.targetId);
    }

    @Override
    public void setSourceId(UUID id) {
        this.sourceId = id;
        setChanged();
        notifyObservers(this.sourceId);
    }

    @Override
    public Queue<DataPacket> getUnsentData() {
        lastMsgPull = Calendar.getInstance();
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
            UUID id = getSourceId();
            if (id != null) {
                return (cc.getTargetId().equals(getTargetId()));
            } else {
                return this == cc;
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

    @Override
    public UUID getSourceId() {
        return sourceId;
    }
}
