package cz.tul.javaccl.history;

import java.net.InetAddress;
import java.util.Date;

/**
 * Data class for storing info about message history. Time is time of logging,
 * not sending (logging is ussualy done right after the message has been sent so
 * the difference should be minimal).
 *
 * @author Petr Ječmen
 */
public class HistoryRecord {

    private final InetAddress ipSource;
    private final InetAddress ipDestination;
    private final Object data;
    private final Object answer;
    private final Date time;
    private final boolean accepted;

    HistoryRecord(final InetAddress ipSource, final InetAddress ipDestination, final Object data, final boolean accepted, final Object answer) {
        this.ipSource = ipSource;
        this.ipDestination = ipDestination;
        this.data = data;
        this.answer = answer;
        this.accepted = accepted;

        time = new Date();
    }

    /**
     * @return ip of sender
     */
    public InetAddress getIpSource() {
        return ipSource;
    }

    /**
     * @return ip of target
     */
    public InetAddress getIpDestination() {
        return ipDestination;
    }

    /**
     * @return sent data
     */
    public Object getData() {
        return data;
    }

    /**
     * @return response to message
     */
    public Object getAnswer() {
        return answer;
    }

    /**
     * @return time of logging
     */
    public Date getTime() {
        return time;
    }

    /**
     * @return true if target confirmed data receive (read and conversion)
     */
    public boolean wasAccepted() {
        return accepted;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("From ");
        sb.append(ipSource.getHostAddress());
        sb.append(" to ");
        sb.append(ipDestination.getHostAddress());
        sb.append(" on ");
        sb.append(time);
        sb.append(". Data - ");
        sb.append(data.toString());
        sb.append(". Was accepted - ");
        sb.append(accepted);

        return sb.toString();
    }
}