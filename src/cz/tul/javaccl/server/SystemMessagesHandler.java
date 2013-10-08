package cz.tul.javaccl.server;

import cz.tul.javaccl.Constants;
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

    private static final Logger log = Logger.getLogger(SystemMessagesHandler.class.getName());
    private final ClientManager clientManager;

    /**
     * @param clientManager client manager
     */
    public SystemMessagesHandler(ClientManager clientManager) {
        if (clientManager != null) {
            this.clientManager = clientManager;
        } else {
            throw new NullPointerException("NULL client manager not allowed.");
        }
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
                            UUID clientId = UUID.randomUUID();
                            clientManager.addClient(dp.getSourceIP(), Integer.parseInt(m.getData().toString()), clientId);
                            log.log(Level.CONFIG, "LOGIN received from " + dp.getSourceIP().getHostAddress() + ", assigning id " + clientId);
                            return clientId;
                        } catch (NumberFormatException ex) {
                            log.log(Level.WARNING, "Illegal login data received - " + m.getData().toString());
                            return GenericResponses.ILLEGAL_DATA;
                        } catch (NullPointerException ex) {
                            log.log(Level.WARNING, "Null login data received.");
                            return GenericResponses.ILLEGAL_DATA;
                        }
                    } else if (header.equals(SystemMessageHeaders.LOGOUT)) {
                        final Object id = m.getData();
                        if (id instanceof UUID) {
                            clientManager.deregisterClient((UUID) id);
                            return GenericResponses.OK;
                        } else {
                            log.log(Level.WARNING, "Invalid client id received - " + id.toString());
                            return GenericResponses.ILLEGAL_DATA;
                        }
                    } else if (header.equals(SystemMessageHeaders.STATUS_CHECK)) {
                        return Constants.ID_SERVER;
                    } else {
                        return GenericResponses.ILLEGAL_HEADER;
                    }
                } else {
                    return GenericResponses.ILLEGAL_HEADER;
                }
            } else {
                if (innerData != null) {
                    log.log(Level.WARNING, "Invalid data received - " + innerData.toString());
                } else {
                    log.log(Level.WARNING, "Received NULL inner data.");
                }
                return GenericResponses.ILLEGAL_DATA;
            }
        } else {
            if (data != null) {
                log.log(Level.WARNING, "Invalid data received - " + data.toString());
            } else {
                log.log(Level.WARNING, "Received NULL data.");
            }
            return GenericResponses.ILLEGAL_DATA;
        }
    }
}
