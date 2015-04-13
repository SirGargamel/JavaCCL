package cz.tul.javaccl.communicator;

import cz.tul.javaccl.GlobalConstants;
import cz.tul.javaccl.GenericResponses;
import cz.tul.javaccl.exceptions.ConnectionException;
import cz.tul.javaccl.exceptions.ConnectionExceptionCause;
import cz.tul.javaccl.history.HistoryManager;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Map;
import java.util.Observable;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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

    private static final Logger LOG = Logger.getLogger(CommunicatorImpl.class.getName());
    private static final Object dummy = "dummyObject";

    /**
     * Create new communicator with given IP and on given port
     *
     * @param targetAddress target IP
     * @param targetPort target port
     * @param sourceId local UUID
     * @return created and initializaed instance of CommunicatorImpl
     */
    public static CommunicatorInner initNewCommunicator(final InetAddress targetAddress, final int targetPort, final UUID sourceId) {
        return new CommunicatorImpl(targetAddress, targetPort, sourceId);
    }
    private final int MSG_PULL_TIME_LIMIT = 2000;
    private final int STATUS_CHECK_TIMEOUT = 250;
    private final int STATUS_CHECK_INTERVAL = 500;
    private final InetAddress address;
    private final int port;
    private final Queue<DataPacket> unsentData;
    private final Map<DataPacket, Object> responses;
    private final UUID sourceId;
    private UUID targetId;
    private Calendar lastStatusUpdateTime;
    private Calendar lastMsgPull;
    private Status status;
    private HistoryManager hm;

    private CommunicatorImpl(final InetAddress address, final int port, final UUID sourceId) throws IllegalArgumentException {
        if (address == null) {
            throw new IllegalArgumentException("NULL address not supported.");
        } else if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port \"" + port + "\"");
        }

        this.address = address;
        this.port = port;
        this.sourceId = sourceId;

        unsentData = new LinkedList<DataPacket>();
        responses = new ConcurrentHashMap<DataPacket, Object>();

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
    }

    @Override
    public InetAddress getAddress() {
        return address;
    }

    @Override
    public Object sendData(final Object data) throws IllegalArgumentException, ConnectionException {
        return sendData(data, GlobalConstants.getDEFAULT_TIMEOUT());
    }

    @Override
    public Object sendData(final Object data, final int timeout) throws IllegalArgumentException, ConnectionException {
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
                response = waitForResponse(dp, timeout);
                readAndReply = true;
            }
        } else {
            throw new ConnectionException(ConnectionExceptionCause.TARGET_OFFLINE);
        }

        if (hm != null) {
            hm.logMessageSend(address, getTargetId(), data, readAndReply, response);
        }

        return response;
    }

    private Object pushDataToOnlineClient(final DataPacket dp, final int timeout) throws ConnectionException {
        Object response = dummy;
        Socket s = null;
        try {
            s = new Socket(address, port);
            s.setSoTimeout(timeout);

            final ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
            out.writeObject(dp);
            out.flush();

            ObjectInputStream in = null;
            try {
                in = new ObjectInputStream(s.getInputStream());
                response = in.readObject();
                LOG.log(Level.FINE, "Received reply from client - " + response);
            } catch (IOException ex) {
                LOG.log(Level.WARNING, "Error receiving response from output socket.");
                LOG.log(Level.FINE, "Error receiving response from output socket.", ex);
            } catch (ClassNotFoundException ex) {
                LOG.log(Level.WARNING, "Unknown class object received.");
                LOG.log(Level.FINE, "Unknown class object received.", ex);
                response = GenericResponses.ILLEGAL_DATA;
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (SocketTimeoutException ex) {
            throw new ConnectionException(ConnectionExceptionCause.TIMEOUT);
        } catch (NotSerializableException ex) {
            throw new IllegalArgumentException("Data for sending (and all of its members) must be serializable (eg. implement Serializable or Externalizable interface.)");
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Cannot write to output socket.");
            LOG.log(Level.FINE, "Cannot write to output socket.", ex);
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (IOException ex) {
                    LOG.warning("Error closing socket.");
                    LOG.log(Level.FINE, "Error closing socket.", ex);
                }
            }
        }
        return response;
    }

    private Object waitForResponse(final DataPacket question, final int timeout) throws ConnectionException {
        final long endTime;
        if (timeout > 0) {
            endTime = System.currentTimeMillis() + timeout;
        } else {
            endTime = Long.MAX_VALUE;
        }

        while (System.currentTimeMillis() < endTime) {
            if (!responses.containsKey(question)) {
                if (checkStatus().equals(Status.ONLINE)) {
                    try {
                        Object response = pushDataToOnlineClient(question, timeout);
                        if (response != dummy) {
                            unsentData.remove(question);
                            responses.put(question, response);
                        }
                    } catch (ConnectionException ex) {
                        LOG.log(Level.WARNING, "Online client connection failed - {0}", ex.getExceptionCause());
                    }

                }
            } else {
                break;
            }

            synchronized (responses) {
                try {
                    responses.wait(STATUS_CHECK_INTERVAL);
                } catch (InterruptedException ex) {
                }
            }
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
        final Object message = new StatusMessage(sourceId);        
        Status stat = Status.OFFLINE;

        Socket s = null;
        try {
            s = new Socket(address, port);
            s.setSoTimeout(STATUS_CHECK_TIMEOUT);

            final ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
            out.writeObject(message);
            out.flush();

            ObjectInputStream in = null;
            try {
                in = new ObjectInputStream(s.getInputStream());
                final Object response = in.readObject();
                if (GenericResponses.OK.equals(response)) {
                    stat = Status.ONLINE;
                } else {
                    LOG.log(Level.WARNING, "Illegal response received for statuc check. Local ID {2}", new Object[]{response, targetId, sourceId});
                }
                result = true;
            } catch (IOException ex) {
                LOG.log(Level.FINE, "Client on IP {0} did not open stream for answer.", address.getHostAddress());
            } catch (ClassNotFoundException ex) {
                LOG.log(Level.WARNING, "Illegal class received from client for KEEP_ALIVE");
                LOG.log(Level.FINE, "Illegal class received from client for KEEP_ALIVE", ex);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (SocketTimeoutException ex) {
            LOG.log(Level.FINE, "Client on IP {0} is not responding to request.", address.getHostAddress());
        } catch (IOException ex) {
            LOG.log(Level.FINE, "Status check IO error.", ex);
        } finally {
            if (s != null && !s.isClosed()) {
                try {
                    s.close();
                } catch (IOException ex) {
                    LOG.warning("Error closing socket.");
                    LOG.log(Level.FINE, "Error closing socket.", ex);
                }
            }
        }

        if (hm != null) {
            hm.logMessageSend(address, getTargetId(), message, result, stat);
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
    public Queue<DataPacket> getUnsentData() {
        lastMsgPull = Calendar.getInstance();
        return unsentData;
    }

    @Override
    public void storeResponse(final DataPacket question, final Object response) {
        synchronized (responses) {
            responses.put(question, response);
            responses.notifyAll();
        }
    }

    @Override
    public boolean equals(Object o) {
        boolean result = false;
        if (o instanceof CommunicatorImpl) {
            CommunicatorImpl cc = (CommunicatorImpl) o;
            UUID id = getSourceId();
            if (id != null) {
                UUID idT = cc.getTargetId();
                UUID idT2 = getTargetId();
                if (idT != null && idT2 != null) {
                    result = (cc.getTargetId().equals(getTargetId()));
                } else {
                    result = idT == idT2;
                }
            } else {
                result = this == cc;
            }

        }
        return result;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 23 * hash + this.address.hashCode();
        hash = 23 * hash + this.port;
        return hash;
    }

    @Override
    public UUID getSourceId() {
        return sourceId;
    }
}
