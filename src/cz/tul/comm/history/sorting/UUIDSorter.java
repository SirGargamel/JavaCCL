package cz.tul.comm.history.sorting;

import cz.tul.comm.history.Record;
import cz.tul.comm.messaging.Message;
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
 * Extracts only Job messages and groups them according to ID.
 *
 * @author Petr Ječmen
 */
public class UUIDSorter extends HistorySorter {

    private static final Logger log = Logger.getLogger(UUIDSorter.class.getName());

    @Override
    public List<Element> sortHistory(final Collection<Record> records, final Document doc) {
        log.fine("Sotring messages by UUID.");

        // group Nodex by UUID
        // and search for job tasks
        final SortedMap<UUID, List<Record>> idGroups = new TreeMap<>();
        List<Record> l;
        Message m;
        Object o;
        for (Record r : records) {
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
            for (Record r : l) {
                group.appendChild(convertRecordToXML(r, doc));
            }

            result.add(group);
        }

        return result;
    }
}
