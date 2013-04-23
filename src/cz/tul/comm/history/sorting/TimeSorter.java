package cz.tul.comm.history.sorting;

import cz.tul.comm.history.HistoryRecord;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Sort XML data according to registration time in ascending order.
 *
 * @author Petr Ječmen
 */
public class TimeSorter extends HistorySorter {

    private static final Logger log = Logger.getLogger(TimeSorter.class.getName());

    @Override
    public List<Element> sortHistory(final Collection<HistoryRecord> records, final Document doc) {
        log.fine("Sorting nodes by time.");

        final List<HistoryRecord> sortedList = new ArrayList<>(records);
        Collections.sort(sortedList, new Comparator<HistoryRecord>() {
            @Override
            public int compare(HistoryRecord o1, HistoryRecord o2) {
                return o1.getTime().compareTo(o2.getTime());
            }
        });

        final List<Element> result = new ArrayList<>(records.size());
        for (HistoryRecord r : sortedList) {
            result.add(convertRecordToXML(r, doc));
        }

        return result;
    }
}
