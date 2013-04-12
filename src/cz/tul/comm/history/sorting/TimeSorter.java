package cz.tul.comm.history.sorting;

import cz.tul.comm.history.Record;
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
    public List<Element> sortHistory(final Collection<Record> records, final Document doc) {
        log.fine("Sorting nodes by time.");

        final List<Record> sortedList = new ArrayList<>(records);
        Collections.sort(sortedList, new Comparator<Record>() {
            @Override
            public int compare(Record o1, Record o2) {
                return o1.getTime().compareTo(o2.getTime());
            }
        });

        final List<Element> result = new ArrayList<>(records.size());
        for (Record r : sortedList) {
            result.add(convertRecordToXML(r, doc));
        }

        return result;
    }
}
