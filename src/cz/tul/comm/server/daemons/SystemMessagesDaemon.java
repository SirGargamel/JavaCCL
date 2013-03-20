package cz.tul.comm.server.daemons;

import cz.tul.comm.messaging.Message;
import cz.tul.comm.messaging.MessageHeaders;
import cz.tul.comm.server.IClientManager;
import cz.tul.comm.socket.IPData;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class SystemMessagesDaemon implements Observer {

    private static final Logger log = Logger.getLogger(SystemMessagesDaemon.class.getName());
    private final IClientManager clientManager;

    public SystemMessagesDaemon(IClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof IPData) {
            final IPData data = (IPData) arg;
            if (data.getData() instanceof Message) {
                final Message m = (Message) data.getData();
                final String header = m.getHeader();

                switch (header) {
                    case MessageHeaders.LOGIN:
                        int port;
                        try {
                            port = Integer.parseInt(m.getData().toString());
                            clientManager.registerClient(data.getIp(), port);                            
                            log.log(Level.CONFIG, "LOGIN received - {0}", m.toString());
                        } catch (NumberFormatException | NullPointerException ex) {
                            log.log(Level.WARNING, "Illegal login data received from {0} - ",
                                    new Object[]{data.getIp().getHostAddress(), m.getData().toString()});
                        }
                        break;
                }
            }
        } else {
            log.log(Level.WARNING, "Invalid data received", arg);
        }
    }
}
