package cz.tul.comm.socket.queue;

import cz.tul.comm.messaging.Message;
import java.util.Queue;
import java.util.UUID;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Petr Ječmen
 */
public class ObjectQueueTest {

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

        public ObjectQueueTest() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of whole class.
     */
    @Test
    public void testObjectQueue() {

        final ObjectQueue queue = new ObjectQueue();

        final Listener l1 = new ListenerImpl();
        final Listener l2 = new ListenerImpl();

        final UUID id1 = UUID.randomUUID();
        final UUID id2 = UUID.randomUUID();
        final UUID id3 = UUID.randomUUID();

        final Queue<Identifiable> q1 = queue.registerListener(id1, l1, false);
        final Queue<Identifiable> q2 = queue.registerListener(id2, l1, false);
        final Queue<Identifiable> q3 = queue.registerListener(id2, l2, false);
        assertNotSame(q2, q3);
        queue.registerListener(id3, l2, false);

        final Message m1 = new Message(id1, "m1", "1");
        final Message m2 = new Message(id2, "m2", "2");
        final Message m2a = new Message(id2, "m2", "2");
        final Message m3 = new Message(id3, "m3", "3");

        queue.storeData(m1);
        queue.storeData(m2);
        queue.storeData(m2a);
        queue.storeData(m3);

        assertEquals(1, q1.size());
        assertEquals(2, q2.size());
        assertEquals(2, q3.size());

        assertEquals(1, queue.getDataQueue(id3, l2).size());
    }

    private static class ListenerImpl implements Listener {

        @Override
        public void receiveData(Identifiable data) {
        }
    }
}