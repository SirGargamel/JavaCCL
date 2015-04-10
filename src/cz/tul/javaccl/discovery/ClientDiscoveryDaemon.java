package cz.tul.javaccl.discovery;

import cz.tul.javaccl.GlobalConstants;
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
public class ClientDiscoveryDaemon extends DiscoveryDaemon {

    private static final Logger LOG = Logger.getLogger(ClientDiscoveryDaemon.class.getName());
    private static final int PAUSE_TIME = 5000;
    private final ClientManager clientManager;

    /**
     * @param clientManager interface for client registration
     * @throws SocketException thrown when daemon could not be created
     */
    public ClientDiscoveryDaemon(final ClientManager clientManager) throws SocketException {
        super();
        this.clientManager = clientManager;
        runThread = true;
    }

    @Override
    public void run() {
        while (runThread) {
            if (pauseThread) {
                pause();
            } else {
                pause(PAUSE_TIME);
            }
        }
    }

    @Override
    public void stopService() {
        super.stopService();
        LOG.fine("ClientDiscoveryDaemon has been stopped.");
    }

    @Override
    protected void receiveBroadcast(final String data, final InetAddress address) {
        // See if the packet holds the right message                    
        if (data.startsWith(GlobalConstants.DISCOVERY_QUESTION)) {
            final String portS = data.substring(GlobalConstants.DISCOVERY_QUESTION.length() + GlobalConstants.DELIMITER.length());
            try {
                final int port = Integer.parseInt(portS);
                try {
                    clientManager.registerClient(address, port);
                } catch (ConnectionException ex) {
                    LOG.log(Level.WARNING, "Could not contact server at IP {0} and port {1} - {2}", new Object[]{address, port, ex.getExceptionCause()});
                }
            } catch (NumberFormatException ex) {
                try {
                    clientManager.registerClient(address, GlobalConstants.DEFAULT_PORT);
                } catch (ConnectionException ex2) {
                    LOG.log(Level.WARNING, "Could not contact server at IP {0} on default port {1} - {2}", new Object[]{address, GlobalConstants.DEFAULT_PORT, ex2.getExceptionCause()});
                }
            }
        }
    }
}
