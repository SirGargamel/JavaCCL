package cz.tul.javaccl.discovery;

import cz.tul.javaccl.Constants;
import cz.tul.javaccl.IService;
import cz.tul.javaccl.exceptions.ConnectionException;
import cz.tul.javaccl.server.ClientManager;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Daemon tryning to discover new clients on local network using UDP broadcast.
 *
 * @author Petr Ječmen
 */
public class ClientDiscoveryDaemon extends DiscoveryDaemon implements IService {

    private static final Logger log = Logger.getLogger(ClientDiscoveryDaemon.class.getName());
    private final ClientManager clientManager;

    /**
     * @param clientManager interface for client registration
     * @throws SocketException thrown when daemon could not be created
     */
    public ClientDiscoveryDaemon(final ClientManager clientManager) throws SocketException {
        super();
        this.clientManager = clientManager;
        run = true;
    }

    @Override
    public void run() {
        while (run) {
            listenForDiscoveryPacket(0);
        }
    }

    @Override
    public void stopService() {
        super.stopService();
        log.fine("ClientDiscoveryDaemon has been stopped.");
    }

    @Override
    protected void receiveBroadcast(final String data, final InetAddress address) {
        // See if the packet holds the right message                    
        if (data.startsWith(Constants.DISCOVERY_QUESTION)) {
            final String portS = data.substring(Constants.DISCOVERY_QUESTION.length() + Constants.DELIMITER.length());
            try {
                final int port = Integer.valueOf(portS);
                try {
                    clientManager.registerClient(address, port);
                } catch (ConnectionException ex) {
                    log.log(Level.WARNING, "Could not contact server at IP " + address + " and port " + port + " - " + ex.getExceptionCause());
                }
            } catch (NumberFormatException ex) {
                try {
                    clientManager.registerClient(address, Constants.DEFAULT_PORT);
                } catch (ConnectionException ex2) {
                    log.log(Level.WARNING, "Could not contact server at IP " + address + " on default port " + Constants.DEFAULT_PORT + " - " + ex2.getExceptionCause());
                }
            }
        }
    }
}
