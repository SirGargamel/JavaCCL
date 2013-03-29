package cz.tul.comm.socket;

import cz.tul.comm.Constants;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.socket.queue.IIdentifiable;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.socket.queue.IListener;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Queue;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

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
        System.out.println("ServerSocket");

        final IListener owner1 = new IListener() {
            @Override
            public void receiveData(IIdentifiable data) {
            }
        };
        final IListener owner2 = new IListener() {
            @Override
            public void receiveData(IIdentifiable data) {
            }
        };

        ServerSocket instance = null;
        try {
            instance = ServerSocket.createServerSocket(Constants.DEFAULT_PORT);
        } catch (IOException ex) {
            fail("Failed to initialize ServerSocket");
        }

        if (instance != null) {
            final Queue<IPData> queueData = instance.addIpListener(InetAddress.getLoopbackAddress(), owner2, false);
            assertNotNull(queueData);

            UUID msgId = UUID.randomUUID();
            final Queue<IIdentifiable> queueMsg = instance.addIdListener(msgId, owner1, false);
            assertNotNull(queueMsg);

            final Communicator c = Communicator.initNewCommunicator(InetAddress.getLoopbackAddress(), Constants.DEFAULT_PORT);

            final Object data1 = "testData";
            final Object data2 = new Integer(50);
            final Message m = new Message(msgId, "head", data2);

            c.sendData(data1);
            c.sendData(data2);
            c.sendData(m);

            int counter = 0;
            for (IPData d : queueData) {
                if (d.getData().equals(data1) || d.getData().equals(data2) || d.getData().equals(m)) {
                    counter++;
                }
            }
            assertEquals(3, counter);

            assertEquals(1, queueMsg.size());
            final IIdentifiable m2 = queueMsg.poll();
            assertEquals(m, m2);
        } else {
            fail("Failed to initialize ServerSocket");
        }
    }
}