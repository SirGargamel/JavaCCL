package cz.tul.comm.client;

import cz.tul.comm.GenericResponses;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.socket.queue.Identifiable;
import cz.tul.comm.socket.queue.Listener;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class handling system messages for client side.
 *
 * @author Petr Ječmen
 */
class SystemMessageHandler implements Listener {

    private static final Logger log = Logger.getLogger(SystemMessageHandler.class.getName());    

    @Override
    public Object receiveData(Identifiable data) {
        if (data instanceof Message) {
            return GenericResponses.OK;
        } else {
            log.log(Level.WARNING, "Illegal data received by client SysMsgHandler - {0}", data.toString());
            return GenericResponses.ILLEGAL_DATA;
        }
    }
}
