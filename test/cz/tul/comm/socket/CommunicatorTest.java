/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.comm.socket;

import cz.tul.comm.communicator.Communicator;
import java.net.InetAddress;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Petr Ječmen
 */
public class CommunicatorTest {

    private static final int PORT_SERVER = 65432;
    private static ServerSocket ss;

    public CommunicatorTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        ss = ServerSocket.createServerSocket(PORT_SERVER);
    }

    @AfterClass
    public static void tearDownClass() {        
        ss.stopService();
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getAddress method, of class Communicator.
     */
    @Test
    public void testCommunicator() {
        System.out.println("Communicator");

        final InetAddress ip = InetAddress.getLoopbackAddress();
        final Communicator instance = new Communicator(ip, PORT_SERVER);

        assertEquals(ip, instance.getAddress());

        final boolean result = instance.sendData("test");
        assertTrue("Target could not receive data", result);
    }
}