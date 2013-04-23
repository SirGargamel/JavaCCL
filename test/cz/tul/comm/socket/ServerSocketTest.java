package cz.tul.comm.socket;

import cz.tul.comm.Constants;
import cz.tul.comm.communicator.CommunicatorImpl;
import cz.tul.comm.communicator.DataPacket;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.socket.queue.Identifiable;
import cz.tul.comm.socket.queue.Listener;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Queue;
import java.util.UUID;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Petr Ječmen
 */
public class ServerSocketTest {

    public ServerSocketTest() {
    }

    /**
     * Test of addMessageHandler method, of class ServerSocket.
     */
    @Test
    public void testServerSocket() {

        final Listener owner1 = new Listener() {
            @Override
            public void receiveData(Identifiable data) {
            }
        };
        final Listener owner2 = new Listener() {
            @Override
            public void receiveData(Identifiable data) {
            }
        };

        ServerSocket instance = null;
        try {
            instance = ServerSocket.createServerSocket(Constants.DEFAULT_PORT, null);
        } catch (IOException ex) {
            fail("Failed to initialize ServerSocket");
        }

        if (instance != null) {
            UUID msgId = UUID.randomUUID();
            final Queue<Identifiable> queueMsg = instance.addIdListener(msgId, owner1, false);
            assertNotNull(queueMsg);

            final CommunicatorImpl c = CommunicatorImpl.initNewCommunicator(InetAddress.getLoopbackAddress(), Constants.DEFAULT_PORT);
            c.setId(UUID.randomUUID());
            final Queue<DataPacket> queueData = instance.addClientListener(c.getId(), owner2, false);
            assertNotNull(queueData);

            final Object data1 = "testData";
            final Object data2 = new Integer(50);
            final Message m = new Message(msgId, "head", data2);

            c.sendData(data1);
            c.sendData(data2);
            c.sendData(m);

            int counter = 0;
            for (DataPacket d : queueData) {
                if (d.getData().equals(data1) || d.getData().equals(data2) || d.getData().equals(m)) {
                    counter++;
                }
            }
            assertEquals(3, counter);

            assertEquals(1, queueMsg.size());
            final Identifiable m2 = queueMsg.poll();
            assertEquals(m, m2);
        } else {
            fail("Failed to initialize ServerSocket");
        }
    }
}