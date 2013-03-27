package cz.tul.comm.socket;

import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.socket.queue.IIdentifiable;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.socket.queue.IListener;
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

    private static final int PORT = 65432;

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

        final ServerSocket instance = ServerSocket.createServerSocket(PORT);        
        final Queue<IPData> queueData = instance.addIpListener(InetAddress.getLoopbackAddress(), owner2, false);
        assertNotNull(queueData);
        
        UUID msgId = UUID.randomUUID();
        final Queue<IIdentifiable> queueMsg = instance.addIdListener(msgId, owner1, false);
        assertNotNull(queueMsg);

        final Communicator c = Communicator.initNewCommunicator(InetAddress.getLoopbackAddress(), PORT);

        final Object data1 = "testData";
        final Object data2 = new Integer(50);
        final Message m = new Message(msgId, "head", data2);

        c.sendData(data1);
        c.sendData(data2);
        c.sendData(m);
                
        assertEquals(3, queueData.size());        

        final Object o1 = queueData.poll().getData();
        final Object o2 = queueData.poll().getData();
        if (o1 instanceof String) {
            assertEquals(o1, data1);
            assertEquals(o2, data2);
        } else {
            assertEquals(o1, data2);
            assertEquals(o2, data1);
        }
        
        assertEquals(1, queueMsg.size());
        final IIdentifiable m2 = queueMsg.poll();
        assertEquals(m, m2);
    }
}