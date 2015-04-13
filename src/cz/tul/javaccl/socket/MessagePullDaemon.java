package cz.tul.javaccl.socket;

import cz.tul.javaccl.GenericResponses;
import cz.tul.javaccl.IService;
import cz.tul.javaccl.communicator.Communicator;
import cz.tul.javaccl.communicator.CommunicatorInner;
import cz.tul.javaccl.communicator.DataPacket;
import cz.tul.javaccl.communicator.Status;
import cz.tul.javaccl.history.HistoryManager;
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
 * Thread for pulling data from client / server, which cannot reach the first
 * side directly (eg. passive mode).
 *
 * @author Petr Ječmen
 */
public class MessagePullDaemon extends Thread implements IService {

    private static final Logger LOG = Logger.getLogger(MessagePullDaemon.class.getName());
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
        Collection<Communicator> comms = new ArrayList<Communicator>(clientLister.getClients().size());
        CommunicatorInner commI;
        MessagePullRequest m;
        InetAddress ipComm;
        int port;
        Object dataIn = null, response = null;
        boolean dataRead;
        Calendar lastTime, now;
        long dif, wait;
        Socket s = null;
        while (run) {
            lastTime = Calendar.getInstance(Locale.getDefault());

            comms.clear();
            comms.addAll(clientLister.getClients());
            for (Communicator comm : comms) {
                if (!run) {
                    break;
                }

                if (comm instanceof CommunicatorInner) {
                    commI = (CommunicatorInner) comm;
                    final UUID id = commI.getSourceId();
                    final Status status = comm.checkStatus();
                    if (status.equals(Status.ONLINE)) {
                        ipComm = comm.getAddress();
                        port = comm.getPort();
                        dataRead = false;

                        m = new MessagePullRequest(id);
                        try {
                            s = new Socket(ipComm, port);
                            ObjectOutputStream out = null;
                            try {
                                out = new ObjectOutputStream(s.getOutputStream());
                                out.writeObject(m);
                                out.flush();

                                ObjectInputStream in = null;
                                try {
                                    in = new ObjectInputStream(s.getInputStream());
                                    try {
                                        dataIn = in.readObject();
                                        dataRead = true;

                                        if (dataIn instanceof GenericResponses) {
                                            if (!dataIn.equals(GenericResponses.OK)) {
                                                LOG.log(Level.WARNING, "Error occured during message pull request - {0}", dataIn.toString());
                                            }
                                        } else if (dataIn instanceof DataPacket) {
                                            final DataPacket dp = (DataPacket) dataIn;
                                            response = dpHandler.handleDataPacket(dp);
                                            LOG.log(Level.FINE, "Pulled message [{0}], responding with {1}", new Object[]{dataIn, response});
                                            out.writeObject(response);
                                            out.flush();
                                        } else {
                                            LOG.log(Level.WARNING, "Pulled unknown data.");
                                        }
                                    } catch (ClassNotFoundException ex) {
                                        LOG.log(Level.WARNING, "Illegal class received.");
                                        LOG.log(Level.FINE, "Illegal class received.", ex);
                                    }
                                } finally {
                                    if (in != null) {
                                        in.close();
                                    }
                                }
                            } finally {
                                if (out != null) {
                                    out.close();
                                }
                            }
                        } catch (SocketTimeoutException ex) {
                            LOG.log(Level.FINE, "Client on IP {0} is not responding to request.", ipComm.getHostAddress());
                        } catch (IOException ex) {
                            LOG.log(Level.WARNING, "Error operating socket.");
                            LOG.log(Level.FINE, "Error operating socket.", ex);
                        } finally {
                            try {
                                if (s != null && !s.isClosed()) {
                                    s.close();
                                }
                            } catch (IOException ex) {
                                LOG.log(Level.WARNING, "Error operating socket.");
                                LOG.log(Level.FINE, "Error operating socket.", ex);
                            }
                        }

                        if (hm != null) {
                            hm.logMessageReceived(ipComm, comm.getTargetId(), dataIn, dataRead, response);
                        }
                    }
                }
            }

            now = Calendar.getInstance(Locale.getDefault());
            dif = now.getTimeInMillis() - lastTime.getTimeInMillis();
            wait = WAIT_TIME - dif;
            if (wait > 0) {
                try {
                    synchronized (this) {
                        this.wait(wait);
                    }
                } catch (InterruptedException ex) {
                    LOG.log(Level.WARNING, "Waiting between message pulls has been interrupted.");
                    LOG.log(Level.FINE, "Waiting between message pulls has been interrupted.", ex);
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
    }

    /**
     * Handle a request from a client (server) to pull data.
     *
     * @param s socket for answering
     * @param pullData received data
     * @param in stream for reading a response
     */
    public void handleMessagePullRequest(final Socket s, MessagePullRequest pullData, ObjectInputStream in) {
        final UUID clientId = pullData.getClientId();
        final Collection<Communicator> comms = new ArrayList<Communicator>(clientLister.getClients());

        Object msg = null;
        CommunicatorInner communicator = null;
        for (Communicator comm : comms) {
            if (comm instanceof CommunicatorInner) {
                communicator = (CommunicatorInner) comm;

                if (clientId.equals(communicator.getSourceId())) {
                    msg = GenericResponses.OK;
                    // no request on itself
                    break;
                }

                if (clientId.equals(comm.getTargetId())) {
                    final Queue<DataPacket> q = communicator.getUnsentData();
                    if (!q.isEmpty()) {
                        msg = q.poll();
                        LOG.log(Level.FINE, "Data prepared for UUID msg pull [" + msg + "].");
                    } else {
                        msg = GenericResponses.OK;
                        LOG.log(Level.FINE, "No data for UUID {0}", clientId);
                    }
                }
            }
        }
        if (msg == null) {
            msg = GenericResponses.UUID_UNKNOWN;
            LOG.log(Level.FINE, "Unknown UUID requested data - {0}", clientId);
        }

        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(s.getOutputStream());
            out.writeObject(msg);
            out.flush();

            if (communicator != null && !(msg instanceof GenericResponses)) {
                try {
                    if (msg instanceof DataPacket) {
                        communicator.storeResponse((DataPacket) msg, in.readObject());
                    }
                } catch (ClassNotFoundException ex) {
                    LOG.log(Level.WARNING, "Unkonwn data class received as reply.");
                    LOG.log(Level.FINE, "Unkonwn data class received as reply.", ex);
                }
            }
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Error operating socket.");
            LOG.log(Level.FINE, "Error operating socket.", ex);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    LOG.warning("Error closing OutputStream.");
                }
            }
        }
    }

    @Override
    public void stopService() {
        run = false;
    }
}
