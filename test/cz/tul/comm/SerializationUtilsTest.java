package cz.tul.comm;

import java.io.File;
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
public class SerializationUtilsTest {

    private static final String TEST_FILE_NAME = "text.tst";
    private static File testFile;

    public SerializationUtilsTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        testFile = new File(TEST_FILE_NAME);
        if (testFile.exists()) {
            testFile.delete();
        }
    }

    @AfterClass
    public static void tearDownClass() {
        if (testFile != null && testFile.exists()) {
            testFile.delete();
        }
    }

    /**
     * Test of saveItemToDisc method, of class SerializationUtils.
     */
    @Test
    public void testSaveAndLoadItemToDisc() throws Exception {
        System.out.println("saveItemToDisc");
        final String data = "Serialization tester";
        if (SerializationUtils.saveItemToDisc(testFile, data)) {
            final Object result = SerializationUtils.loadItemFromDisc(testFile);
            assertEquals(result, data);
        } else {
            fail("Could not serialize data to disc");
        }
    }

    /**
     * Test of saveItemToDiscAsXML method, of class SerializationUtils.
     */
    @Test
    public void testSaveAndLoadItemToDiscAsXML() {
        System.out.println("saveItemToDiscAsXML");
        final String data = "Serialization tester";
        if (SerializationUtils.saveItemToDiscAsXML(testFile, data)) {
            final Object result = SerializationUtils.loadXMLItemFromDisc(testFile);
            assertEquals(result, data);
        } else {
            fail("Could not XML serialize data to disc");
        }

    }
}