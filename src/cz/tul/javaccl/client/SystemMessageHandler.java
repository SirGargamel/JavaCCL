package cz.tul.javaccl.client;

import cz.tul.javaccl.CCLObservable;
import static cz.tul.javaccl.CCLObservable.DEREGISTER;
import cz.tul.javaccl.GlobalConstants;
import cz.tul.javaccl.GenericResponses;
import cz.tul.javaccl.communicator.DataPacketImpl;
import cz.tul.javaccl.messaging.Message;
import cz.tul.javaccl.messaging.SystemMessageHeaders;
import cz.tul.javaccl.socket.IDFilter;
import cz.tul.javaccl.messaging.Identifiable;
import cz.tul.javaccl.socket.Listener;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class handling system messages for client side.
 *
 * @author Petr Ječmen
 */
class SystemMessageHandler extends CCLObservable implements Listener<Identifiable> {

    private static final Logger LOG = Logger.getLogger(SystemMessageHandler.class.getName());
    private final IDFilter idFIlter;
    private final ServerInterface serverInterface;

    public SystemMessageHandler(IDFilter idFIlter, ServerInterface serverInterface) {
        this.idFIlter = idFIlter;
        this.serverInterface = serverInterface;
    }

    @Override
    public Object receiveData(Identifiable data) {
        Object result = GenericResponses.ILLEGAL_DATA;

        if (data instanceof DataPacketImpl) {
            final DataPacketImpl dp = (DataPacketImpl) data;
            final Object innerData = dp.getData();
            if (innerData instanceof Message) {
                final Message m = (Message) innerData;
                final String header = m.getHeader();
                result = GenericResponses.ILLEGAL_HEADER;
                if (header != null) {
                    if (header.equals(SystemMessageHeaders.LOGIN)) {
                        final Object mData = m.getData();
                        if (mData instanceof UUID) {
                            serverInterface.setServerInfo(dp.getSourceIP(), GlobalConstants.DEFAULT_PORT, (UUID) mData);
                            LOG.log(
                                    Level.INFO,
                                    "Registered to new server at " + dp.getSourceIP().getHostAddress() + " on port " + GlobalConstants.DEFAULT_PORT + " with client ID " + mData.toString());
                            result = idFIlter.getLocalID();
                        } else {
                            LOG.log(Level.WARNING, "Illegal data received with LOGIN message - [" + mData.toString() + "].");
                        }
                    } else if (header.equals(SystemMessageHeaders.LOGOUT)) {
                        serverInterface.disconnectFromServer();
                        notifyChange(DEREGISTER, null);
                    }
                }
            }
        }

        return result;
    }
}
