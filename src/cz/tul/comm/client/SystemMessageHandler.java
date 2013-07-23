package cz.tul.comm.client;

import cz.tul.comm.Constants;
import cz.tul.comm.GenericResponses;
import cz.tul.comm.communicator.CommunicatorInner;
import cz.tul.comm.communicator.DataPacketImpl;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.messaging.MessageHeaders;
import cz.tul.comm.socket.IDFilter;
import cz.tul.comm.socket.queue.Identifiable;
import cz.tul.comm.socket.queue.Listener;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class handling system messages for client side.
 *
 * @author Petr Ječmen
 */
class SystemMessageHandler implements Listener {

    private static final Logger log = Logger.getLogger(SystemMessageHandler.class.getName());
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
                switch (m.getHeader()) {
                    case MessageHeaders.STATUS_CHECK:
                        result = idFIlter.getLocalID();
                        break;
                    case MessageHeaders.LOGIN:
                        final Object mData = m.getData();
                        if (mData instanceof UUID) {
                            serverInterface.setServerInfo(dp.getSourceIP(), Constants.DEFAULT_PORT, (UUID) mData);
                            log.log(
                                    Level.CONFIG, 
                                    "Registered to new server at {0} o port {1} with client ID {2}", 
                                    new Object[]{dp.getSourceIP().getHostAddress(), Constants.DEFAULT_PORT, mData.toString()});
                            result = GenericResponses.OK;

                        } else {
                            log.log(Level.WARNING, "Illegal data received with LOGIN message - [{0}].", mData.toString());
                        }
                        break;
                    default:
                        result = GenericResponses.ILLEGAL_HEADER;
                        break;
                }
            }
        }

        return result;
    }
}
