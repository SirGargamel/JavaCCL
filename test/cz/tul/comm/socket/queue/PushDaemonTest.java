/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.comm.socket.queue;

import cz.tul.comm.messaging.Message;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Petr Ječmen
 */
public class PushDaemonTest {

    public PushDaemonTest() {
    }

    /**
     * Test of stopService method, of class PushDaemon.
     */
    @Test
    public void testPushDaemon() {
        System.out.println("stopService");
        final ObjectQueue<Message> q = new ObjectQueue<>();

        final Listener l1 = new Listener();
        final Listener l2 = new Listener();

        final UUID id = UUID.randomUUID();
        final UUID id2 = UUID.randomUUID();
        final Message m1 = new Message(id, "header", "data");
        final Message m2 = new Message(id2, "header", "data2");
        final Message m3 = new Message(id2, "header", "data3");

        q.registerListener(id, l1, true);
        q.registerListener(id2, l2, true);

        q.storeData(m1);
        q.storeData(m2);
        q.storeData(m3);

        try {
            synchronized (this) {
                this.wait(1000);
            }
        } catch (InterruptedException ex) {
            fail("Waiting has been interrupted.");
        }

        assertEquals(1, l1.getCounter());
        assertEquals(2, l2.getCounter());
    }

    private static class Listener implements IListener {

        private int counter;

        @Override
        public void receiveData(IIdentifiable data) {
            counter++;
        }

        public int getCounter() {
            return counter;
        }
    }
}