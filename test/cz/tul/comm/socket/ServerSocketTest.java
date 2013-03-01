package cz.tul.comm.socket;

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

        final IListener<UUID, Message> owner1 = new IListener<UUID, Message>() {

            @Override
            public void receiveData(UUID id, Message data) {                
            }
        };
        final IListener<InetAddress, Object> owner2 = new IListener<InetAddress, Object>() {

            @Override
            public void receiveData(InetAddress id, Object data) {                
            }
        };

        final ServerSocket instance = ServerSocket.createServerSocket(PORT);        
        final Queue<Object> queueData = instance.addDataListener(InetAddress.getLoopbackAddress(), owner2);
        assertNotNull(queueData);
        
        UUID msgId = UUID.randomUUID();
        final Queue<Message> queueMsg = instance.addUUIDListener(msgId, owner1);
        assertNotNull(queueMsg);

        final Communicator c = new Communicator(InetAddress.getLoopbackAddress(), PORT);

        final Object data1 = "testData";
        final Object data2 = new Integer(50);
        final Message m = new Message(msgId, "head", data2);

        c.sendData(data1);
        c.sendData(data2);
        c.sendData(m);
                
        assertEquals(2, queueData.size());        

        final Object o1 = queueData.poll();
        final Object o2 = queueData.poll();
        if (o1 instanceof String) {
            assertEquals(o1, data1);
            assertEquals(o2, data2);
        } else {
            assertEquals(o1, data2);
            assertEquals(o2, data1);
        }
        
        assertEquals(1, queueMsg.size());
        final Message m2 = queueMsg.poll();
        assertEquals(m, m2);
    }
}