package cz.tul.javaccl.socket;

import cz.tul.javaccl.GenericResponses;
import cz.tul.javaccl.communicator.DataPacketImpl;
import cz.tul.javaccl.history.HistoryManager;
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
        super();
        
        if (socket != null) {
            this.socket = socket;
        } else {
            throw new IllegalArgumentException("Socket cannot be null");
        }
        if (dpHandler != null) {
            this.dpHandler = dpHandler;
        } else {
            throw new IllegalArgumentException("ID listeners cannot be null");
        }
        if (mpd != null) {
            this.mpd = mpd;
        } else {
            throw new IllegalArgumentException("MessagePullDaemon cannot be null");
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
        final InetAddress ip = socket.getInetAddress();
        Object dataIn;

        ObjectInputStream in = null;        
        try {
            in = new ObjectInputStream(socket.getInputStream());
            dataIn = in.readObject();

            if (dataIn instanceof DataPacketImpl) {
                final DataPacketImpl packet = (DataPacketImpl) dataIn;                
                packet.setSourceIP(ip);
                final Object response = dpHandler.handleDataPacket(packet);
                sendReply(ip, packet.getSourceId(), packet.getData(), true, response);
            } else if (dataIn instanceof MessagePullRequest) {
                mpd.handleMessagePullRequest(socket, dataIn, in);
            } else {
                LOG.log(Level.WARNING, "Received illegal type of data - {0}", dataIn);
                sendReply(ip, null, dataIn, true, GenericResponses.ILLEGAL_DATA);
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

    private void sendReply(final InetAddress ip, final UUID id, final Object dataIn, final boolean dataRead, final Object response) {
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
