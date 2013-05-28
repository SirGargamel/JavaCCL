package cz.tul.comm.socket;

import cz.tul.comm.GenericResponses;
import cz.tul.comm.IService;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.communicator.CommunicatorInner;
import cz.tul.comm.communicator.DataPacket;
import cz.tul.comm.communicator.Status;
import cz.tul.comm.history.HistoryManager;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.messaging.MessageHeaders;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.Queue;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class MessagePullDaemon extends Thread implements IService {

    private static final Logger log = Logger.getLogger(MessagePullDaemon.class.getName());
    private static final int WAIT_TIME = 100;
    private final ClientLister clientLister;
    private final DataPacketHandler dpHandler;
    private boolean run;
    private HistoryManager hm;

    public MessagePullDaemon(final DataPacketHandler dpHandler, final ClientLister clientLister) {
        if (dpHandler != null) {
            this.dpHandler = dpHandler;
        } else {
            throw new NullPointerException("DatapacketHandler cannot be null.");
        }
        if (clientLister != null) {
            this.clientLister = clientLister;
        } else {
            throw new NullPointerException("ClientLister cannot be null.");
        }

        run = true;
    }

    @Override
    public void run() {
        Collection<Communicator> comms;
        CommunicatorInner commI;
        Message m;
        InetAddress ipComm;
        int port;
        Object dataIn = null, response = null;
        boolean dataRead = false;

        while (run) {
            comms = clientLister.getClients();
            for (Communicator comm : comms) {
                if (comm instanceof CommunicatorInner && comm.checkStatus().equals(Status.ONLINE)) {                    
                    ipComm = comm.getAddress();
                    port = comm.getPort();
                    commI = (CommunicatorInner) comm;
                    m = new Message(MessageHeaders.MSG_PULL_REQUEST, commI.getSourceId());

                    try (final Socket s = new Socket(ipComm, port)) {
                        try (final ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream())) {
                            out.writeObject(m);
                            out.flush();
                            log.log(Level.FINE, "Message pull request sent to {0}:{1}", new Object[]{ipComm.getHostAddress(), port});

                            try (final ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
                                try {
                                    dataIn = in.readObject();
                                    dataRead = true;

                                    if (dataIn instanceof GenericResponses) {
                                        if (!dataIn.equals(GenericResponses.OK)) {
                                            log.log(Level.WARNING, "Error occured during message pull request - {0}", dataIn.toString());
                                        }
                                    } else if (dataIn instanceof DataPacket) {
                                        final DataPacket dp = (DataPacket) dataIn;
                                        response = dpHandler.handleDataPacket(dp);
                                        out.writeObject(response);
                                    }
                                } catch (ClassNotFoundException ex) {
                                    log.log(Level.WARNING, "Illegal class received.", ex);
                                }
                            }
                        }
                    } catch (SocketTimeoutException ex) {
                        log.log(Level.CONFIG, "Client on IP {0} is not responding to request.", ipComm.getHostAddress());
                    } catch (IOException ex) {
                        log.log(Level.WARNING, "Error operating socket.", ex);
                    }

                    if (hm != null) {
                        hm.logMessageReceived(ipComm, dataIn, dataRead, response);
                    }
                }
            }

            synchronized (this) {
                try {
                    this.wait(WAIT_TIME);
                } catch (InterruptedException ex) {
                    log.log(Level.WARNING, "Waiting of MessaPullDaemon has been interrupted.", ex);
                }
            }
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

    public void handleMessagePullRequest(final Socket s, Object pullData) {
        Object msg = null;
        CommunicatorInner communicator = null;

        if (pullData instanceof UUID) {
            Collection<Communicator> comms = clientLister.getClients();

            final UUID id = (UUID) pullData;
            for (Communicator comm : comms) {
                if (comm instanceof CommunicatorInner
                        && id.equals(comm.getTargetId())) {
                    communicator = (CommunicatorInner) comm;
                    final Queue<DataPacket> q = communicator.getUnsentData();
                    if (!q.isEmpty()) {
                        msg = q.poll();
                    } else {
                        msg = GenericResponses.OK;
                        log.log(Level.FINE, "No data for UUID {0}.", id);
                    }
                }
            }
            if (msg == null) {
                msg = GenericResponses.UUID_UNKNOWN;
                log.log(Level.CONFIG, "Unknown UUID requested data - {0}.", id);
            }
        } else {
            msg = GenericResponses.ILLEGAL_DATA;
        }

        try (final ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream())) {
            out.writeObject(msg);
            out.flush();

            if (communicator != null && !(msg instanceof GenericResponses)) {
                try (final ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
                    final Object response = in.readObject();
                    if (msg instanceof DataPacket) {
                        communicator.storeResponse((DataPacket) msg, response);
                    }
                } catch (ClassNotFoundException ex) {
                    log.log(Level.WARNING, "Unkonwn data class received as reply.", ex);
                }
            }
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error operating socket.", ex);
        }
    }

    @Override
    public void stopService() {
        run = false;
    }
}
