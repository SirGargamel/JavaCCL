/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.comm.server;

import cz.tul.comm.Message;
import java.net.InetAddress;
import java.util.UUID;
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
public class DataHandlerTest {
    
    public DataHandlerTest() {
    }   

    /**
     * Test of handleData method, of class DataHandler.
     */
    @Test
    public void testDataHandler() {
        System.out.println("dataHandler");                
        final DataHandler instance = new DataHandler();
        
        final Object owner1 = new Object();
        final Object owner2 = new Object();
        
        final UUID id1 = UUID.randomUUID();
        final UUID id2 = UUID.randomUUID();
        final UUID id3 = UUID.randomUUID();
        
        final InetAddress address = InetAddress.getLoopbackAddress();
        
        instance.registerResponse(address, owner1);
        instance.registerResponse(id1, owner1);
        instance.registerResponse(id2, owner2);
        instance.registerResponse(id3, owner2);
        
        final Message data1 = new Message(id1, "d1", "data");
        final Message data2 = new Message(id1, "d2", "data");
        final Message data3 = new Message(id2, "d3", "data");
        final Message data4 = new Message(id3, "d4", "data");
        
        
        instance.handleData(address, data1);
        instance.handleData(null, data2);
        instance.handleData(address, data3);
        instance.handleData(null, data4);
        
        assertEquals(3, instance.getResponseQueue(owner1).size());
        assertEquals(2, instance.getResponseQueue(owner2).size());
        
        instance.deregisterResponse(id3, owner2);
        instance.deregisterResponse(id2, owner2);
        assertNull(instance.getResponseQueue(owner2));
    }
}