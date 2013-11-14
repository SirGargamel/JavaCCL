/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.javaccl.history;

import cz.tul.javaccl.GlobalConstants;
import cz.tul.javaccl.GenericResponses;
import cz.tul.javaccl.client.ClientImpl;
import cz.tul.javaccl.exceptions.ConnectionException;
import cz.tul.javaccl.history.export.ExportMessage;
import cz.tul.javaccl.history.sorting.DefaultSorter;
import cz.tul.javaccl.history.sorting.IPSorter;
import cz.tul.javaccl.history.sorting.InOutSorter;
import cz.tul.javaccl.history.sorting.UUIDSorter;
import cz.tul.javaccl.messaging.Message;
import cz.tul.javaccl.messaging.SystemMessageHeaders;
import cz.tul.javaccl.server.Server;
import cz.tul.javaccl.server.ServerImpl;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author Petr Jecmen
 */
public class HistoryTest {

    private static final String TEST_FILE_NAME = "export.xml";
    private static final String COMPARE_FILE_NAME = "exportCompare.xml";
    private static final String IP_SOURCE_TEMPLATE = "<IPSource></IPSource>";
    private static final String IP_DESTINATION_TEMPLATE = "<IPDestination></IPDestination>";
    private static final String FIELD_NAME_HEADER = "Header";    
    private static File exportTarget, compareTarget;

    public HistoryTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        exportTarget = new File(System.getProperty("user.home") + File.separator + TEST_FILE_NAME);
        compareTarget = new File(System.getProperty("user.home") + File.separator + COMPARE_FILE_NAME);
    }

    @AfterClass
    public static void tearDownClass() {
        exportTarget.delete();
        compareTarget.delete();
    }

    @Test
    public void testHistoryDirectLogging() throws UnknownHostException {
        System.out.println("logHistoryDirectLoggin");

        InetAddress ipLocal = GlobalConstants.IP_LOOPBACK;

        HistoryManager h = new History();

        synchronized (this) {
            try {
                h.logMessageSend(ipLocal, null, "dataOut", true, GenericResponses.OK);
                this.wait(100);
                h.logMessageReceived(ipLocal, null, GenericResponses.ILLEGAL_DATA, true, "0000-0000");
                this.wait(250);
                h.logMessageReceived(ipLocal, null, "dataIn2", false, "response");
            } catch (InterruptedException ex) {
                fail("Waiting interrupted");
            }
        }

        prepareFile(exportTarget);
        prepareFile(compareTarget);
        assertTrue(h.export(exportTarget, null));
        prepareLocalIp(new File(HistoryTest.class.getResource("testExportDefault.xml").getFile()), compareTarget);
        assertFiles(compareTarget, exportTarget);

        prepareFile(exportTarget);
        prepareFile(compareTarget);
        assertTrue(h.export(exportTarget, new DefaultSorter()));
        prepareLocalIp(new File(HistoryTest.class.getResource("testExportDefault.xml").getFile()), compareTarget);
        assertFiles(compareTarget, exportTarget);

        prepareFile(exportTarget);
        prepareFile(compareTarget);
        assertTrue(h.export(exportTarget, new IPSorter(true)));
        prepareLocalIp(new File(HistoryTest.class.getResource("testExportIpTrue.xml").getFile()), compareTarget);
        assertFiles(compareTarget, exportTarget);

        prepareFile(exportTarget);
        prepareFile(compareTarget);
        assertTrue(h.export(exportTarget, new IPSorter(false)));
        prepareLocalIp(new File(HistoryTest.class.getResource("testExportIpFalse.xml").getFile()), compareTarget);
        assertFiles(compareTarget, exportTarget);

        prepareFile(exportTarget);
        prepareFile(compareTarget);
        assertTrue(h.export(exportTarget, new InOutSorter()));
        prepareLocalIp(new File(HistoryTest.class.getResource("testExportInOut.xml").getFile()), compareTarget);
        assertFiles(compareTarget, exportTarget);
    }

    @Test
    public void testHistoryServerAndClient() throws UnknownHostException {
        System.out.println("logHistoryServerAndClient");

        InetAddress ipLocal = GlobalConstants.IP_LOOPBACK;
        UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID uuid2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440111");

        final Server s = (ServerImpl) ServerImpl.initNewServer();
        History h = (History) s.getHistory();
        h.registerExporter(new ExportMessage());

        final ClientImpl c;
        try {
            s.getHistory().enable(false);
            c = (ClientImpl) ClientImpl.initNewClient(5253);
            c.registerToServer(ipLocal);

            synchronized (this) {
                try {
                    h.enable(true);
                    s.getClient(ipLocal).sendData(new Message(uuid, FIELD_NAME_HEADER, "data1"));
                    this.wait(50);
                    c.sendDataToServer(new Message(uuid, FIELD_NAME_HEADER, "dataBack"));
                    this.wait(50);
                    s.getClient(ipLocal).sendData(new Message(uuid2, FIELD_NAME_HEADER, "data2"));
                } catch (InterruptedException ex) {
                    fail("Waiting interrupted - " + ex.getLocalizedMessage());
                }
            }

            h.enable(false);

            c.stopService();
            s.stopService();

            cleanUp(h.getRecords());

            prepareFile(exportTarget);
            prepareFile(compareTarget);
            assertTrue(s.getHistory().export(exportTarget, new UUIDSorter()));
            prepareLocalIp(new File(HistoryTest.class.getResource("testExportUUID.xml").getFile()), compareTarget);
            assertFiles(compareTarget, exportTarget);
        } catch (IOException ex) {
            fail("Failed to initialize clients - " + ex.getLocalizedMessage());
        } catch (ConnectionException ex) {
            fail("Communication failed - " + ex.getExceptionCause());
        }


    }

    private static void assertFiles(final File expected,
            final File actual) {
        String actualLine, expectedLine, trimmedLine;
        BufferedReader expectedR = null, actualR = null;
        try {
            expectedR = new BufferedReader(new FileReader(expected));
            actualR = new BufferedReader(new FileReader(actual));
            while ((expectedLine = expectedR.readLine()) != null) {
                actualLine = actualR.readLine();
                assertNotNull("Expected had more lines then the actual.", actualLine);

                trimmedLine = expectedLine.trim();
                if (!trimmedLine.startsWith("<Time>") && !trimmedLine.endsWith("ID>")) {
                    assertEquals(expectedLine.trim(), actualLine.trim());
                }
            }
            assertNull("Actual had more lines then the expected.", actualR.readLine());
        } catch (IOException ex) {
            fail("Could not access tested files - " + ex.getLocalizedMessage());
        } finally {
            if (expectedR != null) {
                try {
                    expectedR.close();
                } catch (IOException ex) {
                    Logger.getLogger(HistoryTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (actualR != null) {
                try {
                    actualR.close();
                } catch (IOException ex) {
                    Logger.getLogger(HistoryTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private static void prepareLocalIp(final File fileIn, final File fileOut) {
        // insert local IP at right spots
        BufferedReader in = null;
        BufferedWriter out = null;
        try {
            final String ipSource = "<IPSource>" + InetAddress.getLocalHost().getHostAddress() + "</IPSource>";
            final String ipDest = "<IPDestination>" + InetAddress.getLocalHost().getHostAddress() + "</IPDestination>";

            String line;

            in = new BufferedReader(new FileReader(fileIn));
            out = new BufferedWriter(new FileWriter(fileOut));
            while (in.ready()) {
                line = in.readLine().trim();
                if (line.equals(IP_SOURCE_TEMPLATE)) {
                    out.append(ipSource);
                } else if (line.equals(IP_DESTINATION_TEMPLATE)) {
                    out.append(ipDest);
                } else {
                    out.append(line);
                }
                out.newLine();
            }
        } catch (UnknownHostException ex) {
            fail("Cannot obtain local IP.");
        } catch (IOException ex) {
            fail("Failed to prepare file " + fileIn + " for comparison.");
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    Logger.getLogger(HistoryTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    Logger.getLogger(HistoryTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private static void prepareFile(final File f) {
        // make sure that the file is accessible
        try {
            if (f.exists()) {
                f.delete();
            }
            f.createNewFile();
            if (!f.canWrite() || !f.canRead()) {
                fail("Test files are not accessible.");
            }
        } catch (IOException ex) {
            fail("Test file could not be created.");
        }
    }

    private static void cleanUp(List<HistoryRecord> records) {
        // remove status checks, logins etc.
        Iterator<HistoryRecord> it = records.iterator();

        List<HistoryRecord> remove = new LinkedList<HistoryRecord>();
        Element data;
        NodeList nl;
        for (HistoryRecord hr : records) {
            hr = it.next();
            data = hr.getData();
            nl = data.getElementsByTagName(FIELD_NAME_HEADER);
            for (int i = 0; i < nl.getLength(); i++) {
                if (SystemMessageHeaders.STATUS_CHECK.equals(nl.item(i).getTextContent())) {
                    remove.add(hr);
                }
            }
        }

        records.removeAll(remove);
    }
}