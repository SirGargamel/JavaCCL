/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.comm.persistence;

import cz.tul.comm.ComponentSwitches;
import cz.tul.comm.client.Client;
import cz.tul.comm.client.ClientImpl;
import cz.tul.comm.client.ServerInterface;
import cz.tul.comm.exceptions.ConnectionException;
import cz.tul.comm.server.Server;
import cz.tul.comm.server.ServerImpl;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author Petr Jecmen
 */
public class SettingsTest {

    private static final String TEST_FILE_NAME = "test.xml";
    private static final int CLIENT_PORT = 5253;
    private static Server s;
    private static Client c;
    private static boolean discovery, autoconnect;
    private static File serializationTarget;

    public SettingsTest() {
    }

    @BeforeClass
    public static void setUpClass() {        
        discovery = ComponentSwitches.useClientDiscovery;
        ComponentSwitches.useClientDiscovery = false;

        autoconnect = ComponentSwitches.useClientAutoConnectLocalhost;
        ComponentSwitches.useClientAutoConnectLocalhost = false;

        serializationTarget = new File(System.getProperty("user.home") + File.separator + TEST_FILE_NAME);

        try {
            if (!serializationTarget.exists()) {
                serializationTarget.createNewFile();
            }
            if (!serializationTarget.canWrite() || !serializationTarget.canRead()) {
                fail("Test file is not accessible.");
            }
        } catch (IOException ex) {
            fail("Test file could not be created.");
        }

    }

    @AfterClass
    public static void tearDownClass() {
        serializationTarget.delete();
        
        c.stopService();
        s.stopService();

        ComponentSwitches.useClientDiscovery = discovery;
        ComponentSwitches.useClientAutoConnectLocalhost = autoconnect;
    }
    
    @Before
    public void setUp() {                
        s = ServerImpl.initNewServer();
        c = ClientImpl.initNewClient(CLIENT_PORT);
    }
    
    @After
    public void tearDown() {
        c.stopService();
        s.stopService();
    }

    @Test
    public void testDeserializeServer() {
        System.out.println("deserializeServer");
        boolean result = ServerSettings.deserialize(new File(SettingsTest.class.getResource("testSettingsServer.xml").getFile()), s.getClientManager());
        assertEquals(true, result);

        result = ServerSettings.deserialize(new File(SettingsTest.class.getResource("testSettingsEmpty.xml").getFile()), s.getClientManager());
        assertEquals(true, result);

        result = ServerSettings.deserialize(new File(SettingsTest.class.getResource("testSettingsServerFail.xml").getFile()), s.getClientManager());
        assertEquals(false, result);
    }

    @Test
    public void testSerializeServer() {
        System.out.println("serializeServer");
        s.stopService();
        s = ServerImpl.initNewServer();
        try {
            s.getClientManager().registerClient(InetAddress.getLoopbackAddress(), CLIENT_PORT);
        } catch (IllegalArgumentException ex) {
            fail("Illegal arguments used");
        } catch (ConnectionException ex) {
            fail("Could not connect to client");
        }

        boolean result = ServerSettings.serialize(serializationTarget, s.getClientManager());
        assertEquals(true, result);
        
        assertFiles(new File(SettingsTest.class.getResource("testSettingsServer.xml").getFile()), serializationTarget);
    }

    @Test
    public void testDeserializeClient() {
        System.out.println("deserializeClient");
        boolean result = ClientSettings.deserialize(new File(SettingsTest.class.getResource("testSettingsClient.xml").getFile()), (ServerInterface) c);
        assertEquals(true, result);

        result = ClientSettings.deserialize(new File(SettingsTest.class.getResource("testSettingsClientIpOnly.xml").getFile()), (ServerInterface) c);
        assertEquals(true, result);

        result = ClientSettings.deserialize(new File(SettingsTest.class.getResource("testSettingsEmpty.xml").getFile()), (ServerInterface) c);
        assertEquals(true, result);

        result = ClientSettings.deserialize(new File(SettingsTest.class.getResource("testSettingsClientFail.xml").getFile()), (ServerInterface) c);
        assertEquals(false, result);
    }

    @Test
    public void testSerializeClient() {
        System.out.println("serializeClient");
        c.stopService();
        c = ClientImpl.initNewClient(CLIENT_PORT);
        try {
            c.registerToServer(InetAddress.getLoopbackAddress());
        } catch (IllegalArgumentException ex) {
            fail("Illegal arguments used");
        } catch (ConnectionException ex) {
            fail("Could not connect to client");
        }

        boolean result = ClientSettings.serialize(serializationTarget, c.getServerComm());
        assertEquals(true, result);
        
        assertFiles(new File(SettingsTest.class.getResource("testSettingsClient.xml").getFile()), serializationTarget);
    }

    private static void assertFiles(final File expected,
            final File actual) {
        String actualLine, expectedLine;
        try (BufferedReader expectedR = new BufferedReader(new FileReader(expected))) {
            try (BufferedReader actualR = new BufferedReader(new FileReader(actual))) {
                while ((expectedLine = expectedR.readLine()) != null) {
                    actualLine = actualR.readLine();
                    assertNotNull("Expected had more lines then the actual.", actualLine);
                    assertEquals(expectedLine.trim(), actualLine.trim());
                }
                assertNull("Actual had more lines then the expected.", actualR.readLine());
            }
        } catch (IOException ex) {
            fail("Could not access tested files - " + ex.getLocalizedMessage());
        }
    }
}