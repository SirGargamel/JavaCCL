/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.comm;

import cz.tul.comm.messaging.Message;
import java.awt.image.BufferedImage;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Petr Jecmen
 */
public class UtilsTest {

    public UtilsTest() {
    }

    /**
     * Test of checkSerialization method, of class Utils.
     */
    @Test
    public void testCheckSerialization() {
        System.out.println("checkSerialization");
        
        Object data = "testString";
        assertTrue(Utils.checkSerialization(data));
        
        data = null;
        assertTrue(Utils.checkSerialization(data));

        data = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
        assertFalse(Utils.checkSerialization(data));

        data = new Message("header", new byte[10][10]);
        assertTrue(Utils.checkSerialization(data));

        try {
            data = new Message("header", new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR));
            fail("BufferedImage cannot be serialized, should have failed.");
        } catch (IllegalArgumentException ex) {
        }
    }
}