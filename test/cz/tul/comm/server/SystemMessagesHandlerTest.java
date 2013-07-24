package cz.tul.comm.server;

import cz.tul.comm.GenericResponses;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.communicator.DataPacketImpl;
import cz.tul.comm.exceptions.ConnectionException;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.messaging.MessageHeaders;
import cz.tul.comm.socket.queue.Identifiable;
import java.net.InetAddress;
import java.util.Collection;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Petr Jecmen
 */
public class SystemMessagesHandlerTest {

    @Test
    public void testReceiveData() {
        System.out.println("receiveData");

        ClientMan cm = new ClientMan();
        SystemMessagesHandler instance = new SystemMessagesHandler(cm);

        Message m = new Message(MessageHeaders.LOGIN, null);
        DataPacketImpl dp = new DataPacketImpl(null, null, m);
        Object result = instance.receiveData(dp);
        assertEquals(GenericResponses.ILLEGAL_DATA, result);

        m = new Message(MessageHeaders.LOGIN, "5253,a");
        dp = new DataPacketImpl(null, null, m);
        result = instance.receiveData(dp);
        assertEquals(GenericResponses.ILLEGAL_DATA, result);

        m = new Message(MessageHeaders.LOGIN, "5253");
        dp = new DataPacketImpl(null, null, m);
        dp.setSourceIP(InetAddress.getLoopbackAddress());
        result = instance.receiveData(dp);
        assertTrue(result instanceof UUID);

        m = new Message(MessageHeaders.LOGOUT, "5253");
        dp = new DataPacketImpl(null, null, m);
        result = instance.receiveData(dp);
        assertEquals(GenericResponses.ILLEGAL_DATA, result);

        m = new Message(MessageHeaders.LOGOUT, UUID.randomUUID());
        dp = new DataPacketImpl(null, null, m);
        result = instance.receiveData(dp);
        assertEquals(GenericResponses.OK, result);

        m = new Message("Random header", null);
        dp = new DataPacketImpl(null, null, m);
        result = instance.receiveData(dp);
        assertEquals(GenericResponses.ILLEGAL_HEADER, result);

        dp = new DataPacketImpl(null, null, null);
        result = instance.receiveData(dp);
        assertEquals(GenericResponses.ILLEGAL_DATA, result);

        dp = new DataPacketImpl(null, null, new Object());
        result = instance.receiveData(dp);
        assertEquals(GenericResponses.ILLEGAL_DATA, result);
        
        result = instance.receiveData(null);
        assertEquals(GenericResponses.ILLEGAL_DATA, result);
        
        result = instance.receiveData(new Identifiable() {

            @Override
            public Object getId() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });
        assertEquals(GenericResponses.ILLEGAL_DATA, result);

        assertEquals(2, cm.getActionCount());
    }

    private static class ClientMan implements ClientManager {

        private int actionCount;

        public ClientMan() {
            actionCount = 0;
        }

        public int getActionCount() {
            return actionCount;
        }

        @Override
        public Communicator registerClient(InetAddress adress, int port) throws IllegalArgumentException, ConnectionException {
            actionCount++;
            return null;
        }

        @Override
        public Communicator addClient(InetAddress address, int port, UUID clientId) {
            actionCount++;
            return null;
        }

        @Override
        public void deregisterClient(UUID id) {
            actionCount++;
        }

        @Override
        public Communicator getClient(InetAddress adress, int port) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Communicator getClient(UUID id) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Collection<Communicator> getClients() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
}