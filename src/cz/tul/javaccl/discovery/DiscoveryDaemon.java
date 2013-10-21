package cz.tul.javaccl.discovery;

import cz.tul.javaccl.Constants;
import cz.tul.javaccl.IService;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Jecmen
 */
abstract class DiscoveryDaemon extends Thread implements IService {

    private static final Logger log = Logger.getLogger(DiscoveryDaemon.class.getName());
    private final DatagramSocket s;
    protected boolean run;

    public DiscoveryDaemon() throws SocketException {
        s = new DatagramSocket(Constants.DEFAULT_PORT);
        s.setBroadcast(true);
        s.setSoTimeout(Constants.DEFAULT_TIMEOUT);
    }

    protected void broadcastMessage(final byte[] msg) throws SocketException, IOException {
        // Find the clients using UDP broadcast                
        // Try the 255.255.255.255 first
        final int messageLength = msg.length;
        DatagramPacket sendPacket = new DatagramPacket(msg, messageLength, InetAddress.getByName("255.255.255.255"), Constants.DEFAULT_PORT);
        s.send(sendPacket);

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
                sendPacket = new DatagramPacket(msg, messageLength, broadcast, Constants.DEFAULT_PORT);
                s.send(sendPacket);
            }
        }
    }

    protected void listenForDiscoveryPacket(final int timeout) {
        try {
            // Receive a packet
            byte[] recvBuf = new byte[15000];
            DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
            log.log(Level.FINE, "Starting listening for discovery packets.");
            s.setSoTimeout(timeout);
            s.receive(packet);

            // Packet received            
            String message = new String(packet.getData()).trim();
            log.log(Level.FINE, "Discovery packet received from " + packet.getAddress().getHostAddress() + " - " + message.toString());
            receiveBroadcast(message, packet.getAddress());
        } catch (SocketTimeoutException ex) {
            // everything is OK, we want to check server status again
        } catch (SocketException ex) {
            if (run == false) {
                // everything is fine, we wanted to interrupt socket receive method
            } else {
                log.log(Level.WARNING, "Error operating socket.");
                log.log(Level.FINE, "Error operating socket.", ex);
            }
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error receiving or answering to client discovery packet");
            log.log(Level.FINE, "Error receiving or answering to client discovery packet", ex);
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
        run = false;
        s.close();
    }
}
