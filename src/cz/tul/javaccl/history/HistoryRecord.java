package cz.tul.javaccl.history;

import cz.tul.javaccl.history.export.Exporter;
import java.net.InetAddress;
import java.util.Date;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
    private final Element data;
    private final Element answer;
    private final Date time;
    private final boolean accepted;

    HistoryRecord(final InetAddress ipSource, final InetAddress ipDestination, final Object data, final boolean accepted, final Object answer, final Document doc) {
        this.ipSource = ipSource;
        this.ipDestination = ipDestination;
        this.data = Exporter.exportObject(data, doc);
        this.answer = Exporter.exportObject(answer, doc);
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
    public Element getData() {
        return data;
    }

    /**
     * @return response to message
     */
    public Element getAnswer() {
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
        if (ipSource != null) {
            sb.append(ipSource.getHostAddress());
        } else {
            sb.append("NULL");
        }
        sb.append(" to ");
        if (ipDestination != null) {
            sb.append(ipDestination.getHostAddress());
        } else {
            sb.append("NULL");
        }
        sb.append(" on ");
        sb.append(time);
        sb.append(". Data - ");
        sb.append(data);
        sb.append(". Was accepted - ");
        sb.append(accepted);
        sb.append(" with answer - ");
        sb.append(answer);

        return sb.toString();
    }
}
