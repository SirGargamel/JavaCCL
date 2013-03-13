/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.comm.history;

import java.net.InetAddress;
import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author Petr Ječmen
 */
public class Record {
    
    private final InetAddress ipSource;
    private final InetAddress ipDestination;
    private final Object data;
    private final Date time;
    private final boolean accepted;

    public Record(InetAddress ipSource, InetAddress ipDestination, Object data, final boolean accepted) {
        this.ipSource = ipSource;
        this.ipDestination = ipDestination;
        this.data = data;
        this.accepted = accepted;

        time = new Date();
    }    

    public InetAddress getIpSource() {
        return ipSource;
    }

    public InetAddress getIpDestination() {
        return ipDestination;
    }

    public Object getData() {
        return data;
    }

    public Date getTime() {
        return time;
    }

    public boolean wasAccepted() {
        return accepted;
    }
}
