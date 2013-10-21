package cz.tul.javaccl.socket;

import cz.tul.javaccl.GenericResponses;
import cz.tul.javaccl.IService;
import cz.tul.javaccl.communicator.Communicator;
import cz.tul.javaccl.communicator.CommunicatorInner;
import cz.tul.javaccl.communicator.DataPacket;
import cz.tul.javaccl.communicator.Status;
import cz.tul.javaccl.history.HistoryManager;
import cz.tul.javaccl.messaging.Message;
import cz.tul.javaccl.messaging.SystemMessageHeaders;
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
        Collection<Communicator> comms = new ArrayList<Communicator>(clientLister.getClients().size());
        CommunicatorInner commI;
        Message m;
        InetAddress ipComm;
        int port;
        Object dataIn = null, response = null;
        boolean dataRead = false;
        Calendar lastTime, now;
        long dif, wait;

        while (run) {
            lastTime = Calendar.getInstance(Locale.getDefault());

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

                        Socket s = null;
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
                                                log.log(Level.WARNING, "Error occured during message pull request - " + dataIn.toString());
                                            }
                                        } else if (dataIn instanceof DataPacket) {
                                            final DataPacket dp = (DataPacket) dataIn;
                                            response = dpHandler.handleDataPacket(dp);
                                            log.log(Level.FINE, "Pulled message [" + dataIn + "], responding with " + response);
                                            out.writeObject(response);
                                            out.flush();
                                        } else {
                                            log.log(Level.WARNING, "Pulled unknown data.");
                                        }
                                    } catch (ClassNotFoundException ex) {
                                        log.log(Level.WARNING, "Illegal class received.");
                                        log.log(Level.FINE, "Illegal class received.", ex);
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
                            log.log(Level.FINE, "Client on IP " + ipComm.getHostAddress() + " is not responding to request.");
                        } catch (IOException ex) {
                            log.log(Level.WARNING, "Error operating socket.");
                            log.log(Level.FINE, "Error operating socket.", ex);
                        } finally {
                            try {
                                s.close();
                            } catch (IOException ex) {
                                log.log(Level.WARNING, "Error operating socket.");
                                log.log(Level.FINE, "Error operating socket.", ex);
                            }
                        }

                        if (hm != null) {
                            hm.logMessageReceived(ipComm, dataIn, dataRead, response);
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
                    log.log(Level.WARNING, "Waiting between message pulls has been interrupted.");
                    log.log(Level.FINE, "Waiting between message pulls has been interrupted.", ex);
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
                            log.log(Level.FINE, "Data prepared for UUID msg pull [" + msg + "].");
                        } else {
                            msg = GenericResponses.OK;
                            log.log(Level.FINE, "No data for UUID " + id);
                        }
                    }
                }
            }
            if (msg == null) {
                msg = GenericResponses.UUID_UNKNOWN;
                log.log(Level.FINE, "Unknown UUID requested data - " + id);
            }
        } else {
            msg = GenericResponses.ILLEGAL_DATA;
        }

        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(s.getOutputStream());
            out.writeObject(msg);
            out.flush();

            if (communicator != null && !(msg instanceof GenericResponses)) {
                try {
                    final Object response = in.readObject();
                    if (msg instanceof DataPacket) {
                        communicator.storeResponse((DataPacket) msg, response);
                    }
                } catch (ClassNotFoundException ex) {
                    log.log(Level.WARNING, "Unkonwn data class received as reply.");
                    log.log(Level.FINE, "Unkonwn data class received as reply.", ex);
                }
            }
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error operating socket.");
            log.log(Level.FINE, "Error operating socket.", ex);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    log.warning("Error closing OutputStream.");
                }
            }
        }
    }

    @Override
    public void stopService() {
        run = false;
    }
}
