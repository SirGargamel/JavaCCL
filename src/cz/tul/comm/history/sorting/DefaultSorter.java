package cz.tul.comm.history.sorting;

import cz.tul.comm.history.Record;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * No sorting done to elements.
 *
 * @author Petr Ječmen
 */
public class DefaultSorter extends HistorySorter {

    @Override
    public List<Element> sortHistory(final Collection<Record> records, final Document doc) {       
        final List<Element> result = new ArrayList<>(records.size());
        
        for (Record r : records) {
            result.add(convertRecordToXML(r, doc));
        }
        
        return result;
    }
}
