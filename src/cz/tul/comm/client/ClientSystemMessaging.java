package cz.tul.comm.client;

import cz.tul.comm.messaging.Message;
import cz.tul.comm.messaging.MessageHeaders;
import cz.tul.comm.socket.IPData;
import cz.tul.comm.socket.queue.IIdentifiable;
import cz.tul.comm.socket.queue.IListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Ječmen
 */
class ClientSystemMessaging implements IListener {

    private static final Logger log = Logger.getLogger(ClientSystemMessaging.class.getName());
    private final Comm_Client parent;

    public ClientSystemMessaging(Comm_Client parent) {
        this.parent = parent;
    }

    @Override
    public void receiveData(IIdentifiable data) {
        if (data instanceof IPData) {
            final IPData ipd = (IPData) data;
            if (ipd.getData() instanceof Message) {
                final Message m = (Message) ipd.getData();
                switch (m.getHeader()) {
                    case MessageHeaders.STATUS:
                        final Message response = new Message(m.getId(), m.getHeader(), parent.getStatus());
                        parent.sendData(response);
                        break;
                    default:
                        // nonsystem msg, nothing to do
                        break;
                }
            } else {
                log.log(Level.WARNING, "Non message data received.", data);
            }
        }
    }
}
