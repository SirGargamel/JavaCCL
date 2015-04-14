package cz.tul.javaccl.discovery;

import cz.tul.javaccl.GlobalConstants;
import cz.tul.javaccl.IService;
import cz.tul.javaccl.persistence.Timeout;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Jecmen
 */
abstract class DiscoveryDaemon extends Thread implements IService {

    protected static final Charset CHARSET = Charset.forName("UTF-8");
    protected static final String DISCOVERY_QUESTION = "DISCOVER_CLIENT";
    protected static final String DISCOVERY_INFO = "DISCOVER_SERVER";
    protected static final String DELIMITER = "-";
    private static final Logger LOG = Logger.getLogger(DiscoveryDaemon.class.getName());
    private static final String MULTICAST_IP_S = "230.50.11.2";
    private static final int MULTICAST_PORT = GlobalConstants.DEFAULT_PORT + 1;
    private static final InetAddress MULTICAST_IP;
    private final ExecutorService exec;
    private final DatagramSocket ds;
    private final MulticastSocket ms;
    protected boolean runThread, pauseThread, broadcast;

    static {
        InetAddress group = null;
        try {
            group = InetAddress.getByName(MULTICAST_IP_S);
        } catch (UnknownHostException ex) {
            LOG.warning("Error retreiving multicast address.");
        }
        MULTICAST_IP = group;
    }

    public DiscoveryDaemon() throws SocketException {
        ds = new DatagramSocket(GlobalConstants.DEFAULT_PORT);
        ds.setBroadcast(true);
        ds.setSoTimeout(Timeout.getTimeout(Timeout.TimeoutType.DISCOVERY));

        MulticastSocket msS = null;
        try {
            msS = new MulticastSocket(MULTICAST_PORT);
            msS.joinGroup(MULTICAST_IP);
        } catch (IOException ex) {
            LOG.warning("Error initializing multicast socket.");
        }
        ms = msS;

        exec = Executors.newFixedThreadPool(2);
        exec.execute(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    try {
                        // Receive a packet
                        final byte[] recvBuf = new byte[15000];
                        final DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                        LOG.log(Level.FINE, "Starting listening for discovery packets.");
                        ds.setSoTimeout(Timeout.getTimeout(Timeout.TimeoutType.DISCOVERY));
                        ds.receive(packet);

                        // Packet received            
                        final String message = new String(packet.getData(), CHARSET).trim();
                        LOG.log(Level.FINE, "Broadcast discovery packet received from {0} - {1}", new Object[]{packet.getAddress().getHostAddress(), message});
                        receiveBroadcast(message, packet.getAddress());
                    } catch (SocketTimeoutException ex) {
                        // everything is OK, we want to wait only for limited time
                    } catch (SocketException ex) {
                        if (runThread) {
                            LOG.log(Level.WARNING, "Error operating socket.");
                            LOG.log(Level.FINE, "Error operating socket.", ex);
                        }
                    } catch (IOException ex) {
                        LOG.log(Level.WARNING, "Error receiving or answering to client discovery packet");
                        LOG.log(Level.FINE, "Error receiving or answering to client discovery packet", ex);
                    }
                }
            }
        });
        exec.execute(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    try {
                        // Receive a packet
                        final byte[] recvBuf = new byte[15000];
                        final DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                        LOG.log(Level.FINE, "Starting listening for discovery packets.");
                        ms.setSoTimeout(Timeout.getTimeout(Timeout.TimeoutType.DISCOVERY));
                        ms.receive(packet);

                        // Packet received            
                        final String message = new String(packet.getData(), CHARSET).trim();
                        LOG.log(Level.FINE, "Multicast discovery packet received from {0} - {1}", new Object[]{packet.getAddress().getHostAddress(), message});
                        receiveBroadcast(message, packet.getAddress());
                    } catch (SocketTimeoutException ex) {
                        // everything is OK, we want to wait only for limited time
                    } catch (SocketException ex) {
                        if (runThread) {
                            LOG.log(Level.WARNING, "Error operating socket.");
                            LOG.log(Level.FINE, "Error operating socket.", ex);
                        }
                    } catch (IOException ex) {
                        LOG.log(Level.WARNING, "Error receiving or answering to client discovery packet");
                        LOG.log(Level.FINE, "Error receiving or answering to client discovery packet", ex);
                    }
                }
            }
        });

        broadcast = false;
    }

    protected void broadcastMessage(final byte[] msg) throws SocketException, IOException {
        final int messageLength = msg.length;
        final DatagramPacket sendPacket = new DatagramPacket(msg, messageLength, InetAddress.getByName("255.255.255.255"), GlobalConstants.DEFAULT_PORT);

        if (broadcast) {
            // Find the clients using UDP broadcast                
            // Try the 255.255.255.255 first            
            ds.send(sendPacket);

            // Broadcast the message over all the network interfaces
            final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            NetworkInterface networkInterface;
            InetAddress broadcastIp;
            while (interfaces.hasMoreElements()) {
                networkInterface = interfaces.nextElement();

                if (!networkInterface.isUp()) {
                    continue;
                }

                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    broadcastIp = interfaceAddress.getBroadcast();
                    if (broadcastIp == null) {
                        continue;
                    }

                    // Send the broadcast                    
                    ds.send(new DatagramPacket(msg, messageLength, broadcastIp, GlobalConstants.DEFAULT_PORT));
                }
            }
            broadcast = false;
        } else {
            ms.send(sendPacket);
            broadcast = true;
        }
    }

    /**
     * Handle received String data.
     *
     * @param data received data
     * @param address source IP
     */
    protected abstract void receiveBroadcast(final String data, final InetAddress address);

    @Override
    public void stopService() {
        runThread = false;
        ds.close();
        ms.close();
        exec.shutdownNow();
    }

    public void enable(final boolean enable) {
        if (enable) {
            pauseThread = false;
            synchronized (this) {
                this.notifyAll();
            }
            LOG.fine("DiscoveryDaemon has been enabled.");
        } else {
            pauseThread = true;
            LOG.fine("DiscoveryDaemon has been disabled.");
        }
    }

    protected void pause(final int time) {
        try {
            synchronized (this) {
                this.wait(time);
            }
        } catch (InterruptedException ex) {
            LOG.warning("Waiting of DiscoveryDaemon has been interrupted.");
        }
        pauseThread = false;
    }

    protected void pause() {
        pause(0);
    }
}
