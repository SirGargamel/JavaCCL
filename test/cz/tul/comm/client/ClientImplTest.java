package cz.tul.comm.client;

import cz.tul.comm.ComponentSwitches;
import cz.tul.comm.exceptions.ConnectionException;
import cz.tul.comm.exceptions.ConnectionExceptionCause;
import cz.tul.comm.server.Server;
import cz.tul.comm.server.ServerImpl;
import java.io.IOException;
import java.net.Inet4Address;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author Petr Jecmen
 */
public class ClientImplTest {
    
    private boolean tempDiscovery;
    
    @Before
    public void setUp() {
        tempDiscovery = ComponentSwitches.useClientDiscovery;
        ComponentSwitches.useClientDiscovery = false;
    }
    
    @After
    public void tearDown() {
        ComponentSwitches.useClientDiscovery = tempDiscovery;
    }

    /**
     * Test of setMaxNumberOfConcurrentAssignments method, of class ClientImpl.
     */
    @Test
    public void testSendDataToServer() {
        System.out.println("sendDataToServer");
        try {
            Server s = ServerImpl.initNewServer();
            Client c = ClientImpl.initNewClient(5253);

            try {
                c.sendDataToServer(null);
                fail("Should have failed because of no server info set.");
            } catch (ConnectionException ex) {
                assertEquals(ConnectionExceptionCause.CONNECTION_ERROR, ex.getExceptionCause());
            }

            try {
                c.registerToServer(Inet4Address.getLoopbackAddress());
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
                c.registerToServer(Inet4Address.getLoopbackAddress());
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