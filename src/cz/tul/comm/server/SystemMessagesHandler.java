package cz.tul.comm.server;

import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.communicator.Status;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.messaging.MessageHeaders;
import cz.tul.comm.socket.IPData;
import java.util.Observable;
import java.util.Observer;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Observer handling system messages and its effect.
 *
 * @author Petr Ječmen
 */
public class SystemMessagesHandler implements Observer {

    private static final Logger log = Logger.getLogger(SystemMessagesHandler.class.getName());
    private final IClientManager clientManager;

    /**
     * @param clientManager client manager
     */
    public SystemMessagesHandler(IClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof IPData) {
            final IPData ipData = (IPData) arg;
            final Object data = ipData.getDataPacket().getData();
            if (data instanceof Message) {
                final Message m = (Message) data;
                final String header = m.getHeader();
                switch (header) {
                    case MessageHeaders.LOGIN:
                        int port;
                        try {
                            port = Integer.parseInt(m.getData().toString());
                            UUID clientId = UUID.randomUUID();
                            Communicator c = clientManager.registerClient(ipData.getIp(), port);
                            c.setId(clientId);
                            c.sendData(new Message(m.getId(), header, clientId));
                            log.log(Level.CONFIG, "LOGIN received from {0} - {1}, assigning id {2}", new Object[]{m.toString(), clientId});
                        } catch (NumberFormatException ex) {
                            log.log(Level.WARNING, "Illegal login data received from {0} - {1}",
                                    new Object[]{ipData.getIp().getHostAddress(), m.getData().toString()});
                        } catch (NullPointerException ex) {
                            log.log(Level.WARNING, "Null login data received from {0}",
                                    new Object[]{ipData.getIp().getHostAddress()});
                        }
                        break;
                    case MessageHeaders.STATUS:
                        if (m.getData() instanceof Status) {
                            final Communicator c = clientManager.getClient(ipData.getDataPacket().getClientID());
                            if (c != null) {
                                c.setStatus((Status) m.getData());
                                log.log(Level.CONFIG, "Status message {0} received for IP {1} ,port {2}", new Object[]{m.getData().toString(), c.getAddress(), c.getPort()});
                            }
                        } else {
                            log.log(Level.WARNING, "Invalid status message received {0}", new Object[]{m.getData().toString()});
                        }
                        break;
                    default:
                        // nonsystem message, no action
                        break;
                }
            }
        } else {
            log.log(Level.WARNING, "Invalid data received", arg);
        }
    }
}
