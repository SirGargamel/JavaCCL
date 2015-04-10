package cz.tul.javaccl.socket;

import cz.tul.javaccl.GenericResponses;
import cz.tul.javaccl.communicator.DataPacketImpl;
import cz.tul.javaccl.history.HistoryManager;
import cz.tul.javaccl.messaging.Message;
import cz.tul.javaccl.messaging.SystemMessageHeaders;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Observable;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SocketReader read data from socket and gives them to assigned handlers.
 *
 * @author Petr Ječmen
 */
class SocketReader extends Observable implements Runnable {

    private static final Logger LOG = Logger.getLogger(SocketReader.class.getName());
    private final Socket socket;
    private final DataPacketHandler dpHandler;
    private final MessagePullDaemon mpd;
    private HistoryManager hm;

    /**
     * Create reader, which can read data from socket, tell sender outcome of
     * data reading and store data accoring to IP and ID.
     *
     * @param socket socket for reading
     * @param dpHandler handler for incopming non-system data
     * @param mpd message pull request handler
     */
    SocketReader(
            final Socket socket,
            final DataPacketHandler dpHandler, final MessagePullDaemon mpd) {
        if (socket != null) {
            this.socket = socket;
        } else {
            throw new NullPointerException("Socket cannot be null");
        }
        if (dpHandler != null) {
            this.dpHandler = dpHandler;
        } else {
            throw new NullPointerException("ID listeners cannot be null");
        }
        if (mpd != null) {
            this.mpd = mpd;
        } else {
            throw new NullPointerException("MessagePullDaemon cannot be null");
        }
    }

    /**
     * Register history manager that will store info about received messages.
     *
     * @param hm instance of history manager
     */
    public void registerHistory(final HistoryManager hm) {
        this.hm = hm;
    }

    @Override
    public void run() {
        boolean dataRead = false;
        final InetAddress ip = socket.getInetAddress();
        Object dataIn = null;

        ObjectInputStream in = null;
        UUID id = null;
        try {
            in = new ObjectInputStream(socket.getInputStream());
            dataIn = in.readObject();
            dataRead = true;

            if (dataIn instanceof DataPacketImpl) {
                final DataPacketImpl dp = (DataPacketImpl) dataIn;
                id = dp.getSourceId();
                dp.setSourceIP(ip);
                final Object response = dpHandler.handleDataPacket(dp);
                sendReply(ip, id, dp.getData(), dataRead, response);
            } else if (dataIn instanceof Message) {
                final Message m = (Message) dataIn;

                final String header = m.getHeader();
                if (header != null && header.equals(SystemMessageHeaders.MSG_PULL_REQUEST)) {
                    mpd.handleMessagePullRequest(socket, m.getData(), in);
                } else {
                    LOG.log(Level.WARNING, "Received Message with unidentifined header - " + m.toString());
                    sendReply(ip, null, dataIn, dataRead, GenericResponses.ILLEGAL_HEADER);
                }
            } else {
                LOG.log(Level.WARNING, "Received data is not an instance of DataPacket or Message - " + dataIn);
                sendReply(ip, null, dataIn, dataRead, GenericResponses.ILLEGAL_DATA);
            }
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Error reading data from socket.");
            LOG.log(Level.FINE, "Error reading data from socket.", ex);
            sendReply(ip, null, null, false, GenericResponses.CONNECTION_ERROR);
        } catch (ClassNotFoundException ex) {
            LOG.log(Level.WARNING, "Invalid data received from sender.");
            LOG.log(Level.FINE, "Invalid data received from sender.", ex);
            sendReply(ip, null, null, false, GenericResponses.ILLEGAL_DATA);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    LOG.log(Level.WARNING, "Error operating socket.");
                    LOG.log(Level.FINE, "Error operating socket.", ex);
                }
            }
        }

        try {
            if (!socket.isClosed() || !socket.isInputShutdown() || !socket.isOutputShutdown()) {
                socket.close();
            }
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Error closing socket.");
            LOG.log(Level.FINE, "Error closing socket.", ex);
        }
    }

    private void sendReply(final InetAddress ip, final UUID id, Object dataIn, boolean dataRead, final Object response) {
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(response);
            out.flush();
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Error writing result data to socket.");
            LOG.log(Level.FINE, "Error writing result data to socket.", ex);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    LOG.log(Level.WARNING, "Error closing socket.");
                    LOG.log(Level.FINE, "Error closing socket.", ex);
                }
            }
        }

        if (hm != null) {
            hm.logMessageReceived(ip, id, dataIn, dataRead, response);
        }
    }
}
