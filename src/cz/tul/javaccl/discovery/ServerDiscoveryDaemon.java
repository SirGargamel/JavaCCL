package cz.tul.javaccl.discovery;

import cz.tul.javaccl.Constants;
import cz.tul.javaccl.client.ServerInterface;
import cz.tul.javaccl.communicator.Communicator;
import cz.tul.javaccl.exceptions.ConnectionException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client side of DicoveryDaemon. Listens for server broadcast and responds to
 * them. Listens only when no server is aviable.
 *
 * @author Petr Ječmen
 */
public class ServerDiscoveryDaemon extends DiscoveryDaemon {

    private static final Logger log = Logger.getLogger(ServerDiscoveryDaemon.class.getName());
    private final ServerInterface sr;
    private boolean run;

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
            if (!sr.isServerUp()) {
                listenForDiscoveryPacket();
            } else {
                broadcastServerInfo();
            }

            synchronized (this) {
                try {
                    this.wait(DELAY);
                } catch (InterruptedException ex) {
                    log.warning("Waiting of ServerDiscoveryDaemon has been interrupted.");
                }
            }
        }
        s.close();
    }

    @Override
    public void stopService() {
        super.stopService();
        run = false;
        log.fine("ServerDiscoveryDaemon has been stopped.");
    }

    private void listenForDiscoveryPacket() {
        try {
            //Receive a packet
            byte[] recvBuf = new byte[15000];
            DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
            log.log(Level.FINE, "Starting listening for discovery packets");
            s.receive(packet);

            //Packet received                    
            log.log(Level.CONFIG, "Discovery packet received from " + packet.getAddress().getHostAddress());
            String message = new String(packet.getData()).trim();
            log.log(Level.CONFIG, "Discovery packet received from " + packet.getAddress().getHostAddress() + " - " + message.toString());

            //See if the packet holds the right message                    
            if (message.startsWith(Constants.DISCOVERY_QUESTION)) {
                final String portS = message.substring(Constants.DISCOVERY_QUESTION.length() + Constants.DELIMITER.length());
                try {
                    final int port = Integer.valueOf(portS);
                    try {
                        sr.registerToServer(packet.getAddress(), port);
                    } catch (ConnectionException ex) {
                        log.log(Level.WARNING, "Could not contact server at IP " + packet.getAddress() + " and port " + port);
                    }
                } catch (NumberFormatException ex) {
                    try {
                        sr.registerToServer(packet.getAddress(), Constants.DEFAULT_PORT);
                    } catch (ConnectionException ex2) {
                        log.log(Level.WARNING, "Could not contact server at IP " + packet.getAddress() + " on default port " + Constants.DEFAULT_PORT);
                    }
                }
            } else if (message.startsWith(Constants.DISCOVERY_INFO)) {
                final String ipAndPort = message.substring(Constants.DISCOVERY_QUESTION.length() + Constants.DELIMITER.length());
                final String ipS = ipAndPort.substring(0, ipAndPort.indexOf(Constants.DELIMITER));
                final String portS = ipAndPort.substring(ipAndPort.indexOf(Constants.DELIMITER) + 1);
                try {
                    final int port = Integer.valueOf(portS);
                    final InetAddress ip = InetAddress.getByName(ipS);
                    try {
                        sr.registerToServer(ip, port);
                    } catch (ConnectionException ex) {
                        log.log(Level.WARNING, "Could not contact server at IP " + ip + " and port " + port);
                    }
                } catch (NumberFormatException ex) {
                    try {
                        sr.registerToServer(packet.getAddress(), Constants.DEFAULT_PORT);
                    } catch (ConnectionException ex2) {
                        log.log(Level.WARNING, "Could not contact server at IP " + packet.getAddress() + " on default port " + Constants.DEFAULT_PORT);
                    }
                }
            }
        } catch (SocketTimeoutException ex) {
            // everything is OK, we want to check server status again
        } catch (SocketException ex) {
            if (run == false) {
                // everything is fine, wa wanted to interrupt socket receive method
            } else {
                log.log(Level.WARNING, "Error operating socket.", ex);
            }
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error receiving or answering to client discovery packet", ex);
        }
    }

    private void broadcastServerInfo() {
        final Communicator comm = sr.getServerComm();
        StringBuilder sb = new StringBuilder();
        sb.append(Constants.DISCOVERY_INFO);
        sb.append(Constants.DELIMITER);
        sb.append(comm.getAddress().getHostAddress());
        sb.append(Constants.DELIMITER);
        sb.append(comm.getPort());

        try {
            broadcastMessage(sb.toString().getBytes());
        } catch (SocketException ex) {
            log.log(Level.WARNING, "Error while checking status of network interfaces", ex);
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error writing to socket", ex);
        }
    }
}
