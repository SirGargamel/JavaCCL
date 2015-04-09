package cz.tul.javaccl.discovery;

import cz.tul.javaccl.GlobalConstants;
import cz.tul.javaccl.IService;
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
    private static final Logger log = Logger.getLogger(DiscoveryDaemon.class.getName());    
    private static final String MULTICAST_IP_S= "230.50.11.2";
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
            log.warning("Error retreiving multicast address.");
        }
        MULTICAST_IP = group;
    }

    public DiscoveryDaemon() throws SocketException {
        ds = new DatagramSocket(GlobalConstants.DEFAULT_PORT);
        ds.setBroadcast(true);
        ds.setSoTimeout(GlobalConstants.getDEFAULT_TIMEOUT());

        MulticastSocket msS = null;
        try {
            msS = new MulticastSocket(MULTICAST_PORT);
            msS.joinGroup(MULTICAST_IP);
        } catch (IOException ex) {
            log.warning("Error initializing multicast socket.");
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
                        log.log(Level.FINE, "Starting listening for discovery packets.");
                        ds.setSoTimeout(GlobalConstants.getDEFAULT_TIMEOUT());
                        ds.receive(packet);

                        // Packet received            
                        final String message = new String(packet.getData(), CHARSET).trim();
                        log.log(Level.FINE, "Broadcast discovery packet received from " + packet.getAddress().getHostAddress() + " - " + message);
                        receiveBroadcast(message, packet.getAddress());
                    } catch (SocketTimeoutException ex) {
                        // everything is OK, we want to wait only for limited time
                    } catch (SocketException ex) {
                        if (runThread) {
                            log.log(Level.WARNING, "Error operating socket.");
                            log.log(Level.FINE, "Error operating socket.", ex);
                        }
                    } catch (IOException ex) {
                        log.log(Level.WARNING, "Error receiving or answering to client discovery packet");
                        log.log(Level.FINE, "Error receiving or answering to client discovery packet", ex);
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
                        log.log(Level.FINE, "Starting listening for discovery packets.");
                        ms.setSoTimeout(GlobalConstants.getDEFAULT_TIMEOUT());
                        ms.receive(packet);

                        // Packet received            
                        final String message = new String(packet.getData(), CHARSET).trim();
                        log.log(Level.FINE, "Multicast discovery packet received from {0} - {1}", new Object[]{packet.getAddress().getHostAddress(), message});
                        receiveBroadcast(message, packet.getAddress());
                    } catch (SocketTimeoutException ex) {
                        // everything is OK, we want to wait only for limited time
                    } catch (SocketException ex) {
                        if (runThread) {
                            log.log(Level.WARNING, "Error operating socket.");
                            log.log(Level.FINE, "Error operating socket.", ex);
                        }
                    } catch (IOException ex) {
                        log.log(Level.WARNING, "Error receiving or answering to client discovery packet");
                        log.log(Level.FINE, "Error receiving or answering to client discovery packet", ex);
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

    public void enable(boolean enable) {
        if (enable) {
            pauseThread = false;
            synchronized (this) {
                this.notifyAll();
            }
            log.fine("DiscoveryDaemon has been enabled.");
        } else {
            pauseThread = true;
            log.fine("DiscoveryDaemon has been disabled.");
        }
    }

    protected void pause(final int time) {
        try {
            synchronized (this) {
                this.wait(time);
            }
        } catch (InterruptedException ex) {
            log.warning("Waiting of DiscoveryDaemon has been interrupted.");
        }
        pauseThread = false;
    }
    
    protected void pause() {
        pause(0);
    }
}
