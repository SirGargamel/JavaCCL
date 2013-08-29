package cz.tul.comm.server;

import cz.tul.comm.Constants;
import cz.tul.comm.GenericResponses;
import cz.tul.comm.communicator.DataPacketImpl;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.messaging.SystemMessageHeaders;
import cz.tul.comm.messaging.Identifiable;
import cz.tul.comm.socket.Listener;
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
                switch (header) {
                    case SystemMessageHeaders.LOGIN:
                        try {
                            UUID clientId = UUID.randomUUID();
                            clientManager.addClient(dp.getSourceIP(), Integer.parseInt(m.getData().toString()), clientId);
                            log.log(Level.CONFIG, "LOGIN received from {0}, assigning id {1}", new Object[]{dp.getSourceIP().getHostAddress(), clientId});
                            return clientId;
                        } catch (NumberFormatException ex) {
                            log.log(Level.WARNING, "Illegal login data received - {0}",
                                    new Object[]{m.getData().toString()});
                            return GenericResponses.ILLEGAL_DATA;
                        } catch (NullPointerException ex) {
                            log.log(Level.WARNING, "Null login data received.");
                            return GenericResponses.ILLEGAL_DATA;
                        }
                    case SystemMessageHeaders.LOGOUT:
                        final Object id = m.getData();
                        if (id instanceof UUID) {
                            clientManager.deregisterClient((UUID) id);                            
                            return GenericResponses.OK;
                        } else {
                            log.log(Level.WARNING, "Invalid client id received - {0}", id.toString());
                            return GenericResponses.ILLEGAL_DATA;
                        }
                    case SystemMessageHeaders.STATUS_CHECK:
                        return Constants.ID_SERVER;
                    default:
                        return GenericResponses.ILLEGAL_HEADER;
                }
            } else {
                if (innerData != null) {
                    log.log(Level.WARNING, "Invalid data received - {0}", innerData.toString());
                } else {
                    log.log(Level.WARNING, "Received NULL inner data.");
                }
                return GenericResponses.ILLEGAL_DATA;
            }
        } else {
            if (data != null) {
                log.log(Level.WARNING, "Invalid data received - {0}", data.toString());
            } else {
                log.log(Level.WARNING, "Received NULL data.");
            }
            return GenericResponses.ILLEGAL_DATA;
        }
    }
}
