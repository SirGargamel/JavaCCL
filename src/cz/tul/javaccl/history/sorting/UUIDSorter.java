package cz.tul.javaccl.history.sorting;

import cz.tul.javaccl.history.HistoryRecord;
import cz.tul.javaccl.messaging.Message;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Extracts messages and groups them according to their IDs.
 *
 * @author Petr Ječmen
 */
public class UUIDSorter extends HistorySorter {

    private static final Logger log = Logger.getLogger(UUIDSorter.class.getName());

    @Override
    public List<Element> sortHistory(final Collection<HistoryRecord> records, final Document doc) {
        log.fine("Sotring messages by UUID.");

        // group Nodes by UUID        
        final SortedMap<UUID, List<HistoryRecord>> idGroups = new TreeMap<>();
        List<HistoryRecord> l;
        Message m;
        Object o;
        for (HistoryRecord r : records) {
            o = r.getData();
            if (o instanceof Message) {
                m = (Message) o;
                l = idGroups.get(m.getId());
                if (l == null) {
                    l = new ArrayList<>();
                    idGroups.put(m.getId(), l);
                }
                l.add(r);
            }
        }

        final List<Element> result = new ArrayList<>(records.size());
        Element group;
        for (UUID id : idGroups.keySet()) {
            group = doc.createElement("Group");
            group.setAttribute("UUID", id.toString());

            l = idGroups.get(id);
            for (HistoryRecord r : l) {
                group.appendChild(convertRecordToXML(r, doc));
            }

            result.add(group);
        }

        return result;
    }
}
