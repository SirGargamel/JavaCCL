package cz.tul.comm.socket;

import cz.tul.comm.GenericResponses;
import cz.tul.comm.communicator.DataPacket;
import cz.tul.comm.history.HistoryManager;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.messaging.MessageHeaders;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SocketReader read data from socket and gives them to assigned handlers.
 *
 * @author Petr Ječmen
 */
class SocketReader extends Observable implements Runnable {

    private static final Logger log = Logger.getLogger(SocketReader.class.getName());    
    private final Socket socket;
    private final DataPacketHandler dpHandler;
    private final MessagePullDaemon mpd;
    private HistoryManager hm;

    /**
     * Create reader, which can read data from socket, tell sender outcome of
     * data reading and store data accoring to IP and ID.
     *
     * @param socket socket for reading
     * @param dataStorageIP IP listeners storage
     * @param dataStorageId ID listeners storage
     */
    SocketReader(
            final Socket socket,
            final DataPacketHandler dpHandler,final MessagePullDaemon mpd) {
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
        log.fine("History registered.");
    }

    @Override
    public void run() {
        boolean dataReadV = false;
        final InetAddress ip = socket.getInetAddress();
        Object dataInV = null;
        try {
            final ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            dataInV = in.readObject();
            dataReadV = true;
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error reading data from socket.", ex);
        } catch (ClassNotFoundException ex) {
            log.log(Level.WARNING, "Invalid data received from sender.", ex);
        }
        final Object dataIn = dataInV;
        final boolean dataRead = dataReadV;

        if (dataIn instanceof DataPacket) {
            final DataPacket dp = (DataPacket) dataIn;
            dp.setSourceIP(ip);
            sendReply(ip, dataIn, dataRead, dpHandler.handleDataPacket(dp));
        } else if (dataIn instanceof Message) {
            final Message m = (Message) dataIn;
            switch (m.getHeader()) {
                case (MessageHeaders.KEEP_ALIVE):
                    log.log(Level.FINE, "keepAlive received from {0}", ip.getHostAddress());
                    sendReply(ip, dataIn, dataRead, GenericResponses.OK);
                    break;
                case (MessageHeaders.MSG_PULL_REQUEST):
                    mpd.handleMessagePullRequest(socket, m.getData());
                    break;
                default:
                    log.log(Level.WARNING, "Received Message with unidentifined header - {0}", new Object[]{m.toString()});
                    sendReply(ip, dataIn, dataRead, GenericResponses.UNKNOWN_DATA);
                    break;
            }
        } else {
            log.log(Level.WARNING, "Received data is not an instance of DataPacket - {0}", new Object[]{dataIn});
            sendReply(ip, dataIn, dataRead, GenericResponses.ILLEGAL_DATA);
        }

        try {
            socket.close();
        } catch (Exception ex) {
            log.log(Level.FINE, "Error closing socket.", ex);
        }
    }

    private void sendReply(final InetAddress ip, Object dataIn, boolean dataRead, final Object response) {
        try (final ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.writeObject(response);
            out.flush();
            log.log(Level.FINE, "Reply sent.");
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error writing result data to socket.", ex);
        }

        if (hm != null) {
            hm.logMessageReceived(ip, dataIn, dataRead, response);
        }
    }
}
