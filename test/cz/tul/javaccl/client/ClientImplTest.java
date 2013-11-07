package cz.tul.javaccl.client;

import cz.tul.javaccl.GlobalConstants;
import cz.tul.javaccl.exceptions.ConnectionException;
import cz.tul.javaccl.exceptions.ConnectionExceptionCause;
import cz.tul.javaccl.server.Server;
import cz.tul.javaccl.server.ServerImpl;
import java.io.IOException;
import java.net.Inet4Address;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Petr Jecmen
 */
public class ClientImplTest {

    /**
     * Test of setMaxNumberOfConcurrentAssignments method, of class ClientImpl.
     */
    @Test
    public void testSendDataToServer() {
        System.out.println("sendDataToServer");
        try {
            Server s = ServerImpl.initNewServer(5251);
            Client c = ClientImpl.initNewClient(5253);

            try {
                c.sendDataToServer(null);
                fail("Should have failed because of no server info set.");
            } catch (ConnectionException ex) {
                assertEquals(ConnectionExceptionCause.CONNECTION_ERROR, ex.getExceptionCause());
            }

            try {
                c.registerToServer(Inet4Address.getByName(GlobalConstants.IP_LOOPBACK), 5251);
            } catch (ConnectionException ex) {
                fail("Registration failed.");
            }

            s.stopService();

            try {
                synchronized (this) {
                    this.wait(1000);
                }
                c.sendDataToServer(null);
                fail("Should have failed because server is offline.");
            } catch (ConnectionException ex) {
                assertEquals(ConnectionExceptionCause.CONNECTION_ERROR, ex.getExceptionCause());
            } catch (InterruptedException ex) {
                fail("Waiting has been interrupted.");
            }


            s = ServerImpl.initNewServer();
            try {
                c.registerToServer(Inet4Address.getByName(GlobalConstants.IP_LOOPBACK));
            } catch (ConnectionException ex) {
                fail("Registration failed.");
            }

            try {
                assertNotNull(c.sendDataToServer("a"));
            } catch (ConnectionException ex) {
                fail("Could not reach server.");
            }

            c.stopService();
            s.stopService();
        } catch (IOException ex) {
            fail("Failed to initialize client.");
        }
    }
}