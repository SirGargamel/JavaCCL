package cz.tul.comm.communicator;

import cz.tul.comm.GenericResponses;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.messaging.MessageHeaders;
import cz.tul.comm.socket.queue.Identifiable;
import cz.tul.comm.socket.queue.Listener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Collection;
import java.util.Queue;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class MessagePullReceiver implements Listener {

    private static final Logger log = Logger.getLogger(MessagePullReceiver.class.getName());
    private Collection<CommunicatorImpl> comms;

    public void setComms(final Collection<CommunicatorImpl> comms) {
        this.comms = comms;
    }

    public void handleMessagePullRequest(final Socket s, Object pullData) {
        Object msg = null;
        CommunicatorImpl communicator = null;
        if (pullData instanceof UUID) {
            final UUID id = (UUID) pullData;
            for (CommunicatorImpl comm : comms) {
                if (id.equals(comm.getId())) {
                    communicator = comm;
                    final Queue<DataPacket> q = comm.getUnsentData();
                    if (!q.isEmpty()) {
                        msg = q.poll();
                    } else {
                        msg = GenericResponses.OK;
                    }
                }
            }
            if (msg == null) {
                msg = GenericResponses.UUID_UNKNOWN;
            }
        }

        try (final ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream())) {
            out.writeObject(msg);
            out.flush();

            if (communicator != null) {
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
    public Object receiveData(Identifiable data) {
        Object response = null;

        if (data instanceof Message) {
            final Message m = (Message) data;
            if (m.getHeader().equals(MessageHeaders.MSG_PULL_REQUEST)) {
                final Object mData = m.getData();
                if (mData instanceof UUID) {
                    final UUID id = (UUID) mData;
                    for (CommunicatorImpl comm : comms) {
                        if (id.equals(comm.getId())) {
                            final Queue<DataPacket> q = comm.getUnsentData();
                            if (!q.isEmpty()) {
                                response = q.poll();
                            } else {
                                response = GenericResponses.OK;
                            }
                        }
                    }
                    if (response == null) {
                        response = GenericResponses.UUID_UNKNOWN;
                    }
                }
            }
        }

        if (response == null) {
            response = GenericResponses.ILLEGAL_DATA;
        }

        return response;
    }
}
