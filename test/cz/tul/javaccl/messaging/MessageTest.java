/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.javaccl.messaging;

import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Petr Jecmen
 */
public class MessageTest {

    /**
     * Test of getId method, of class Message.
     */
    @Test
    public void testGetId() {
        System.out.println("getId");
        UUID id = UUID.randomUUID();
        Message instance = new Message(id, null, null);
        UUID result = instance.getId();
        assertEquals(id, result);
    }

    /**
     * Test of getHeader method, of class Message.
     */
    @Test
    public void testGetHeader() {
        System.out.println("getHeader");
        String header = "header";
        Message instance = new Message(header, null);
        String result = instance.getHeader();
        assertEquals(header, result);
    }

    /**
     * Test of getData method, of class Message.
     */
    @Test
    public void testGetData() {
        System.out.println("getData");

        Object data = "msgData";
        Message instance = new Message("", data);
        Object result = instance.getData();
        assertEquals(data, result);
    }

    /**
     * Test of toString method, of class Message.
     */
    @Test
    public void testToString() {
        System.out.println("toString");
        UUID id = UUID.randomUUID();
        String header = "header";
        Object data = "data";
        Message instance = new Message(id, header, data);
        String expResult = id.toString().concat(" - ").concat(header).concat(" - ").concat(data.toString());
        String result = instance.toString();
        assertEquals(expResult, result);
    }

    /**
     * Test of equals method, of class Message.
     */
    @Test
    public void testEquals() {
        System.out.println("equals");
        UUID id = UUID.randomUUID();
        String header = "header";
        Object data = "data";
        Message instance = new Message(id, header, data);
        
        Object o = new Message(id, header, data.toString());
        
        assertTrue(instance.equals(o));
    }
}