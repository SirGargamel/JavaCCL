package cz.tul.javaccl.server;

import cz.tul.javaccl.GenericResponses;
import cz.tul.javaccl.communicator.DataPacketImpl;
import cz.tul.javaccl.messaging.Message;
import cz.tul.javaccl.messaging.SystemMessageHeaders;
import cz.tul.javaccl.messaging.Identifiable;
import cz.tul.javaccl.socket.Listener;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Observer handling system messages and its effects.
 *
 * @author Petr Ječmen
 */
public class SystemMessagesHandler implements Listener<Identifiable> {

    private static final Logger LOG = Logger.getLogger(SystemMessagesHandler.class.getName());
    private final ClientManager clientManager;
    private final UUID localId;

    /**
     * @param clientManager client manager
     * @param localId local UUID
     */
    public SystemMessagesHandler(ClientManager clientManager, final UUID localId) {
        if (clientManager != null) {
            this.clientManager = clientManager;
        } else {
            throw new NullPointerException("NULL client manager not allowed.");
        }
        
        this.localId = localId;
    }

    @Override
    public Object receiveData(Identifiable data) {
        if (data instanceof DataPacketImpl) {
            final DataPacketImpl dp = (DataPacketImpl) data;
            final Object innerData = dp.getData();
            if (innerData instanceof Message) {
                final Message m = (Message) innerData;
                final String header = m.getHeader();
                if (header != null) {
                    if (header.equals(SystemMessageHeaders.LOGIN)) {
                        try {
                            final Integer port = Integer.parseInt(m.getData().toString());
                            clientManager.addClient(dp.getSourceIP(), port, dp.getSourceId());
                            LOG.log(Level.FINE, "LOGIN received from {0} with id {1}", new Object[]{dp.getSourceIP().getHostAddress(), dp.getSourceId()});
                            return localId;                        
                        } catch (NumberFormatException ex) {
                            LOG.log(Level.WARNING, "Illegal login data received - {0}", m.getData());
                            return GenericResponses.ILLEGAL_DATA;
                        } catch (NullPointerException ex) {
                            LOG.log(Level.WARNING, "Null login data received.");
                            return GenericResponses.ILLEGAL_DATA;
                        }
                    } else if (header.equals(SystemMessageHeaders.LOGOUT)) {
                        final Object id = m.getData();
                        if (id instanceof UUID) {
                            clientManager.deregisterClient((UUID) id);
                            return GenericResponses.OK;
                        } else {
                            LOG.log(Level.WARNING, "Invalid client id received - {0}", id);
                            return GenericResponses.ILLEGAL_DATA;
                        }
                    } else if (header.equals(SystemMessageHeaders.STATUS_CHECK)) {
                        return localId;
                    } else {
                        return GenericResponses.ILLEGAL_HEADER;
                    }
                } else {
                    return GenericResponses.ILLEGAL_HEADER;
                }
            } else {
                if (innerData != null) {
                    LOG.log(Level.WARNING, "Invalid data received - {0}", innerData);
                } else {
                    LOG.log(Level.WARNING, "Received NULL inner data.");
                }
                return GenericResponses.ILLEGAL_DATA;
            }
        } else {
            if (data != null) {
                LOG.log(Level.WARNING, "Invalid data received - {0}", data);
            } else {
                LOG.log(Level.WARNING, "Received NULL data.");
            }
            return GenericResponses.ILLEGAL_DATA;
        }
    }
}
