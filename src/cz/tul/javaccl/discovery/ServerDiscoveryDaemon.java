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

    private static final Logger LOG = Logger.getLogger(ServerDiscoveryDaemon.class.getName());
    private static final int PAUSE_TIME = 5000;
    private final ServerInterface sr;

    /**
     *
     * @param sr interface for settings new server settings
     * @throws SocketException thrown when client could not be created
     */
    public ServerDiscoveryDaemon(final ServerInterface sr) throws SocketException {
        super();
        this.sr = sr;
        runThread = true;
    }

    @Override
    public void run() {
        while (runThread) {
            if (pauseThread) {
                pause();
            }
            if (sr.getServerComm() == null) {
                broadcastServerDiscovery();
            }
            pause(PAUSE_TIME);
        }
    }

    @Override
    public void stopService() {
        super.stopService();
        LOG.fine("ServerDiscoveryDaemon has been stopped.");
    }

    private void broadcastServerDiscovery() {
        StringBuilder sb = new StringBuilder();
        sb.append(DISCOVERY_QUESTION);
        sb.append(DELIMITER);
        sb.append(sr.getLocalSocketPort());

        try {
            broadcastMessage(sb.toString().getBytes(CHARSET));
        } catch (SocketException ex) {
            LOG.log(Level.WARNING, "Error while checking status of network interfaces");
            LOG.log(Level.FINE, "Error while checking status of network interfaces", ex);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Error operating socket.");
            LOG.log(Level.FINE, "Error operating socket.", ex);
        }
    }

    @Override
    protected void receiveBroadcast(String data, InetAddress address) {
        // See if the packet holds the right message                    
        if (sr.getServerComm() != null && data.startsWith(DISCOVERY_QUESTION)) {
            broadcastServerInfo();
        } else if (sr.getServerComm() == null && data.startsWith(DISCOVERY_INFO)) {
            final String ipAndPort = data.substring(DISCOVERY_QUESTION.length() + DELIMITER.length());
            final String ipS = ipAndPort.substring(0, ipAndPort.indexOf(DELIMITER));
            final String portS = ipAndPort.substring(ipAndPort.indexOf(DELIMITER));                         
            try {
                final InetAddress ipAddr = InetAddress.getByName(ipS);

                int port;
                try {
                    port = Integer.parseInt(portS);
                } catch (NumberFormatException ex) {
                    LOG.log(Level.WARNING, "Illegal port number received - {0}", portS);
                    port = GlobalConstants.DEFAULT_PORT;
                }
                
                try {
                    sr.registerToServer(ipAddr, port);
                } catch (ConnectionException ex) {
                    LOG.warning("Error contacting server.");
                    LOG.log(Level.FINE, "Error contacting server.", ex);
                }
            } catch (UnknownHostException ex) {
                LOG.log(Level.WARNING, "Illegal server IP received - {0}", ipS);
            }
        }
    }

    private void broadcastServerInfo() {
        final Communicator comm = sr.getServerComm();
        if (comm != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(DISCOVERY_INFO);
            sb.append(DELIMITER);
            InetAddress address = comm.getAddress();
            try {
                if (InetAddress.getByName("127.0.0.1").equals(address)) {
                    address = InetAddress.getLocalHost();
                }
            } catch (UnknownHostException ex) {
                address = comm.getAddress();
            }
            sb.append(address);
            sb.append(DELIMITER);
            sb.append(comm.getPort());

            try {
                broadcastMessage(sb.toString().getBytes(CHARSET));
            } catch (SocketException ex) {
                LOG.log(Level.WARNING, "Error while checking status of network interfaces");
                LOG.log(Level.FINE, "Error while checking status of network interfaces", ex);
            } catch (IOException ex) {
                LOG.log(Level.WARNING, "Error operating socket.");
                LOG.log(Level.FINE, "Error operating socket.", ex);
            }
        }
    }
}
