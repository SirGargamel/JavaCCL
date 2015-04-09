package cz.tul.javaccl.history.sorting;

import cz.tul.javaccl.history.HistoryRecord;
import static cz.tul.javaccl.history.sorting.HistorySorter.convertRecordToXML;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Sorter for XML data according to IP address of sender or receiver.
 *
 * @author Petr Ječmen
 */
public class IPSorter extends HistorySorter {

    private static final Logger log = Logger.getLogger(IPSorter.class.getName());

    private static int compareIp(final InetAddress adr1, final InetAddress adr2) {
        byte[] ba1 = adr1.getAddress();
        byte[] ba2 = adr2.getAddress();

        // general ordering: ipv4 before ipv6
        if (ba1.length < ba2.length) {
            return -1;
        }
        if (ba1.length > ba2.length) {
            return 1;
        }

        // we have 2 ips of the same type, so we have to compare each byte
        for (int i = 0; i < ba1.length; i++) {
            int b1 = unsignedByteToInt(ba1[i]);
            int b2 = unsignedByteToInt(ba2[i]);
            if (b1 == b2) {
                continue;
            }
            if (b1 < b2) {
                return -1;
            } else {
                return 1;
            }
        }
        return 0;
    }

    private static int unsignedByteToInt(byte b) {
        return (int) b & 0xFF;
    }
    private final boolean byDestination;

    /**
     * @param byDestination true for sorting by IP of receiver
     */
    public IPSorter(final boolean byDestination) {
        this.byDestination = byDestination;
    }

    @Override
    public List<Element> sortHistory(final Collection<HistoryRecord> records, final Document doc) {
        log.fine("Sorting nodes by time.");

        final List<HistoryRecord> sortedList = new ArrayList<HistoryRecord>(records);
        final Comparator<HistoryRecord> comp = byDestination ? new IpSorterDestination() : new IpSorterSource();

        Collections.sort(sortedList, comp);

        final List<Element> result = new ArrayList<Element>(records.size());
        for (HistoryRecord r : sortedList) {
            result.add(convertRecordToXML(r, doc));
        }

        return result;
    }

    private static final class IpSorterDestination implements Comparator<HistoryRecord>, Serializable {

        @Override
        public int compare(final HistoryRecord record1, final HistoryRecord record2) {
            return compareIp(record1.getIpDestination(), record2.getIpDestination());
        }
    }

    private static final class IpSorterSource implements Comparator<HistoryRecord>, Serializable {

        @Override
        public int compare(final HistoryRecord record1, final HistoryRecord record2) {
            return compareIp(record1.getIpSource(), record2.getIpSource());
        }
    }
}
