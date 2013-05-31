package cz.tul.comm.client;

import cz.tul.comm.GenericResponses;
import cz.tul.comm.communicator.DataPacket;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.messaging.MessageHeaders;
import cz.tul.comm.socket.IDFilter;
import cz.tul.comm.socket.queue.Identifiable;
import cz.tul.comm.socket.queue.Listener;
import java.util.logging.Logger;

/**
 * Class handling system messages for client side.
 *
 * @author Petr Ječmen
 */
class SystemMessageHandler implements Listener {

    private static final Logger log = Logger.getLogger(SystemMessageHandler.class.getName());
    private IDFilter idFIlter;

    public SystemMessageHandler(IDFilter idFIlter) {
        this.idFIlter = idFIlter;
    }
    

    @Override
    public Object receiveData(Identifiable data) {
        Object result = GenericResponses.ILLEGAL_DATA;

        if (data instanceof DataPacket) {
            final DataPacket dp = (DataPacket) data;
            final Object innerData = dp.getData();
            if (innerData instanceof Message) {
                final Message m = (Message) innerData;
                switch (m.getHeader()) {
                    case MessageHeaders.STATUS_CHECK:                        
                        result = idFIlter.getLocalID();
                        break;
                }
            }
        }

        return result;
    }
}
