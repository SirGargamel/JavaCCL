package cz.tul.comm.socket;

import cz.tul.comm.server.DataHandler;
import java.net.InetAddress;
import java.util.Queue;
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

        final DataHandler handler = new DataHandler();

        final Object owner = new Object();
        handler.registerResponse(InetAddress.getLoopbackAddress(), owner);

        final ServerSocket instance = ServerSocket.createServerSocket(PORT);
        instance.addMessageHandler(handler);

        final Communicator c = new Communicator(InetAddress.getLoopbackAddress(), PORT);

        final Object data1 = "testData";
        final Object data2 = 50;

        c.sendData(data1);
        c.sendData(data2);

        final Queue<Object> queue = handler.getResponseQueue(owner);
        assertNotNull(queue);
        assertEquals(2, queue.size());

        final Object o1 = queue.poll();
        final Object o2 = queue.poll();
        if (o1 instanceof String) {
            assertEquals(o1, data1);
            assertEquals(o2, data2);
        } else {
            assertEquals(o1, data2);
            assertEquals(o2, data1);
        }
    }
}