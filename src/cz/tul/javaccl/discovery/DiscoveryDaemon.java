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

    private static final Logger log = Logger.getLogger(DiscoveryDaemon.class.getName());
    private static final String multicastGroupS = "230.50.11.2";
    private static final InetAddress multicastGroup;
    private final ExecutorService exec;
    private final DatagramSocket ds;
    private final MulticastSocket ms;
    protected boolean run, pause;

    static {
        InetAddress group = null;
        try {
            group = InetAddress.getByName(multicastGroupS);
        } catch (UnknownHostException ex) {
            log.warning("Error retreiving multicast address.");
        }
        multicastGroup = group;
    }

    public DiscoveryDaemon() throws SocketException {
        ds = new DatagramSocket(GlobalConstants.DEFAULT_PORT);
        ds.setBroadcast(true);
        ds.setSoTimeout(GlobalConstants.getDEFAULT_TIMEOUT());

        MulticastSocket msS = null;
        try {
            msS = new MulticastSocket(GlobalConstants.DEFAULT_PORT + 1);
            msS.joinGroup(multicastGroup);
        } catch (IOException ex) {
            log.warning("Error initializing multicast socket.");
        }
        ms = msS;

        exec = Executors.newFixedThreadPool(2);
    }

    protected void broadcastMessage(final byte[] msg) throws SocketException, IOException {
        // Find the clients using UDP broadcast                
        // Try the 255.255.255.255 first
        final int messageLength = msg.length;
        DatagramPacket sendPacket = new DatagramPacket(msg, messageLength, InetAddress.getByName("255.255.255.255"), GlobalConstants.DEFAULT_PORT);
        ds.send(sendPacket);

        // Broadcast the message over all the network interfaces
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();

            if (!networkInterface.isUp()) {
                continue;
            }

            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                InetAddress broadcast = interfaceAddress.getBroadcast();
                if (broadcast == null) {
                    continue;
                }

                // Send the broadcast
                sendPacket = new DatagramPacket(msg, messageLength, broadcast, GlobalConstants.DEFAULT_PORT);
                ds.send(sendPacket);
            }
        }

        ms.send(sendPacket);
    }

    protected void listenForDiscoveryPacket(final int timeout) {
        // broadcast listening
        exec.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    // Receive a packet
                    byte[] recvBuf = new byte[15000];
                    DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                    log.log(Level.FINE, "Starting listening for discovery packets.");
                    ds.setSoTimeout(timeout);
                    ds.receive(packet);

                    // Packet received            
                    String message = new String(packet.getData()).trim();
                    log.log(Level.FINE, "Broadcast discovery packet received from " + packet.getAddress().getHostAddress() + " - " + message.toString());
                    receiveBroadcast(message, packet.getAddress());
                } catch (SocketTimeoutException ex) {
                    // everything is OK, we want to wait only for limited time
                } catch (SocketException ex) {
                    if (run == true) {
                        log.log(Level.WARNING, "Error operating socket.");
                        log.log(Level.FINE, "Error operating socket.", ex);
                    }
                } catch (IOException ex) {
                    log.log(Level.WARNING, "Error receiving or answering to client discovery packet");
                    log.log(Level.FINE, "Error receiving or answering to client discovery packet", ex);
                }
            }
        });
        // multicast listening
        exec.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    // Receive a packet
                    byte[] recvBuf = new byte[15000];
                    DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                    log.log(Level.FINE, "Starting listening for discovery packets.");
                    ms.setSoTimeout(timeout);
                    ms.receive(packet);

                    // Packet received            
                    String message = new String(packet.getData()).trim();
                    log.log(Level.FINE, "Multicast discovery packet received from " + packet.getAddress().getHostAddress() + " - " + message.toString());
                    receiveBroadcast(message, packet.getAddress());
                } catch (SocketTimeoutException ex) {
                    // everything is OK, we want to wait only for limited time
                } catch (SocketException ex) {
                    if (run == true) {
                        log.log(Level.WARNING, "Error operating socket.");
                        log.log(Level.FINE, "Error operating socket.", ex);
                    }
                } catch (IOException ex) {
                    log.log(Level.WARNING, "Error receiving or answering to client discovery packet");
                    log.log(Level.FINE, "Error receiving or answering to client discovery packet", ex);
                }
            }
        });
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
        run = false;
        ds.close();
    }
    
    public void enable(boolean enable) {
        if (enable) {
            pause = false;
            synchronized (this) {
                this.notify();
            }
            log.fine("DiscoveryDaemon has been enabled.");
        } else {
            pause = true;
            log.fine("DiscoveryDaemon has been disabled.");
        }
    }

    protected void pause() {
        try {
            synchronized (this) {
                this.wait();
            }
        } catch (InterruptedException ex) {
            log.warning("Waiting of DiscoveryDaemon has been interrupted.");
        }
        pause = false;
    }
}
