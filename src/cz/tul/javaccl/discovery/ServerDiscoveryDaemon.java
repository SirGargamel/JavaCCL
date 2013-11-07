package cz.tul.javaccl.discovery;

import cz.tul.javaccl.GlobalConstants;
import cz.tul.javaccl.client.ServerInterface;
import cz.tul.javaccl.communicator.Communicator;
import cz.tul.javaccl.exceptions.ConnectionException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client side of DiscoveryDaemon. Listens for server broadcast and responds to
 * them. Listens only when no server is aviable.
 *
 * @author Petr Ječmen
 */
public class ServerDiscoveryDaemon extends DiscoveryDaemon {

    private static final Logger log = Logger.getLogger(ServerDiscoveryDaemon.class.getName());
    private static final int DELAY = 5000;
    private final ServerInterface sr;

    /**
     *
     * @param sr interface for settings new server settings
     * @throws SocketException thrown when client could not be created
     */
    public ServerDiscoveryDaemon(final ServerInterface sr) throws SocketException {
        super();
        this.sr = sr;
        run = true;
    }

    @Override
    public void run() {
        while (run) {
            if (sr.getServerComm() == null) {
                broadcastServerDiscovery();
            }
            listenForDiscoveryPacket(DELAY);
        }
    }

    @Override
    public void stopService() {
        super.stopService();
        log.fine("ServerDiscoveryDaemon has been stopped.");
    }

    private void broadcastServerDiscovery() {
        StringBuilder sb = new StringBuilder();
        sb.append(GlobalConstants.DISCOVERY_QUESTION);
        sb.append(GlobalConstants.DELIMITER);
        sb.append(sr.getLocalSocketPort());

        try {
            broadcastMessage(sb.toString().getBytes());
        } catch (SocketException ex) {
            log.log(Level.WARNING, "Error while checking status of network interfaces");
            log.log(Level.FINE, "Error while checking status of network interfaces", ex);
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error operating socket.");
            log.log(Level.FINE, "Error operating socket.", ex);
        }
    }

    @Override
    protected void receiveBroadcast(String data, InetAddress address) {
        // See if the packet holds the right message                    
        if (sr.getServerComm() != null && data.startsWith(GlobalConstants.DISCOVERY_QUESTION)) {
            broadcastServerInfo();
        } else if (sr.getServerComm() == null && data.startsWith(GlobalConstants.DISCOVERY_INFO)) {
            final String ipAndPort = data.substring(GlobalConstants.DISCOVERY_QUESTION.length() + GlobalConstants.DELIMITER.length());
            final String ipS = ipAndPort.substring(0, ipAndPort.indexOf(GlobalConstants.DELIMITER));
            final String portS = ipAndPort.substring(ipAndPort.indexOf(GlobalConstants.DELIMITER));
            InetAddress ip = null;
            int port = GlobalConstants.getDEFAULT_PORT();
            try {
                ip = InetAddress.getByName(ipS);
                port = Integer.valueOf(portS);

            } catch (UnknownHostException ex) {
                log.warning("Illegal server IP received - " + ipS);
            } catch (NumberFormatException ex) {
                log.warning("Illegal port number received - " + portS);
            }

            if (ip != null) {
                try {
                    sr.registerToServer(ip, Integer.valueOf(port));
                } catch (ConnectionException ex) {
                    log.warning("Error contacting server.");
                    log.log(Level.FINE, "Error contacting server.", ex);
                }
            }
        }
    }

    private void broadcastServerInfo() {
        final Communicator comm = sr.getServerComm();
        if (comm != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(GlobalConstants.DISCOVERY_INFO);
            sb.append(GlobalConstants.DELIMITER);
            InetAddress address = comm.getAddress();
            try {
                if (InetAddress.getByName("127.0.0.1").equals(address)) {
                    address = InetAddress.getLocalHost();
                }
            } catch (UnknownHostException ex) {
                address = comm.getAddress();
            }
            sb.append(address);
            sb.append(GlobalConstants.DELIMITER);
            sb.append(comm.getPort());

            try {
                broadcastMessage(sb.toString().getBytes());
            } catch (SocketException ex) {
                log.log(Level.WARNING, "Error while checking status of network interfaces");
                log.log(Level.FINE, "Error while checking status of network interfaces", ex);
            } catch (IOException ex) {
                log.log(Level.WARNING, "Error operating socket.");
                log.log(Level.FINE, "Error operating socket.", ex);
            }
        }
    }
}
