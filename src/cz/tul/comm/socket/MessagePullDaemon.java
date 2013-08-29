package cz.tul.comm.socket;

import cz.tul.comm.GenericResponses;
import cz.tul.comm.IService;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.communicator.CommunicatorInner;
import cz.tul.comm.communicator.DataPacket;
import cz.tul.comm.communicator.Status;
import cz.tul.comm.history.HistoryManager;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.messaging.SystemMessageHeaders;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Locale;
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
    private static final int WAIT_TIME = 500;
    private final ClientLister clientLister;
    private final DataPacketHandler dpHandler;
    private boolean run;
    private HistoryManager hm;

    /**
     * New instance of daemon for pulling messages.
     *
     * @param dpHandler handler of data packets
     * @param clientLister interface for obtaining list of clients
     */
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
        Collection<Communicator> comms = new ArrayList<>(clientLister.getClients().size());
        CommunicatorInner commI;
        Message m;
        InetAddress ipComm;
        int port;
        Object dataIn = null, response = null;
        boolean dataRead = false;
        Calendar lastTime = Calendar.getInstance(Locale.getDefault()), now;
        long dif, wait;

        while (run) {
            now = Calendar.getInstance(Locale.getDefault());
            dif = now.getTimeInMillis() - lastTime.getTimeInMillis();
            wait = WAIT_TIME - dif;
            if (wait > 0) {
                try {
                    synchronized (this) {
                        this.wait(wait);
                    }
                } catch (InterruptedException ex) {
                    log.log(Level.WARNING, "Waiting between message pulls has been interrupted.", ex);
                }
            }

            comms.clear();
            comms.addAll(clientLister.getClients());
            for (Communicator comm : clientLister.getClients()) {
                if (!run) {
                    break;
                }

                if (comm instanceof CommunicatorInner) {
                    commI = (CommunicatorInner) comm;
                    final UUID id = commI.getSourceId();
                    if (id == null) {
                        continue;
                    }

                    final Status status = comm.getStatus();
                    if (status.equals(Status.ONLINE)) {
                        ipComm = comm.getAddress();
                        port = comm.getPort();

                        m = new Message(SystemMessageHeaders.MSG_PULL_REQUEST, id);

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
                                            log.log(Level.FINE, "Pulled message [{0}], responding with {1}.", new Object[]{dataIn, response});
                                            out.writeObject(response);
                                            out.flush();
                                        } else {
                                            log.log(Level.WARNING, "Pulled unknown data.");
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

    /**
     * Handle a request from a client (server) to pull data.
     *
     * @param s socket for answering
     * @param pullData received data
     * @param in stream for reading a response
     */
    public void handleMessagePullRequest(final Socket s, Object pullData, ObjectInputStream in) {
        Object msg = null;
        CommunicatorInner communicator = null;

        if (pullData instanceof UUID) {
            final UUID id = (UUID) pullData;
            Collection<Communicator> comms = clientLister.getClients();

            for (Communicator comm : comms) {
                if (comm instanceof CommunicatorInner) {
                    communicator = (CommunicatorInner) comm;

                    if (id.equals(communicator.getSourceId())) {
                        msg = GenericResponses.OK;
                        // no request on itself
                        break;
                    }

                    if (id.equals(comm.getTargetId())) {
                        final Queue<DataPacket> q = communicator.getUnsentData();
                        if (!q.isEmpty()) {
                            msg = q.poll();
                            log.log(Level.FINE, "Data prepared for UUID msg pull [{0}].", msg);
                        } else {
                            msg = GenericResponses.OK;
                            log.log(Level.FINE, "No data for UUID {0}.", id);
                        }
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
                try {
                    final Object response = in.readObject();
                    if (msg instanceof DataPacket) {
                        communicator.storeResponse((DataPacket) msg, response);
                    }
                } catch (ClassNotFoundException ex) {
                    log.log(Level.WARNING, "Unkonwn data class received as reply.", ex);
                }
            }
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error operating socket.\n", ex);
        }
    }

    @Override
    public void stopService() {
        run = false;
    }
}
