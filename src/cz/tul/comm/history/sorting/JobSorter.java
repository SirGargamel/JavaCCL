package cz.tul.comm.history.sorting;

import cz.tul.comm.history.HistoryRecord;
import cz.tul.comm.messaging.Message;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Extracts only Job messages and groups them according to ID.
 *
 * @author Petr Ječmen
 */
public class JobSorter extends HistorySorter {

    private static final Logger log = Logger.getLogger(JobSorter.class.getName());

    @Override
    public List<Element> sortHistory(final Collection<HistoryRecord> records, final Document doc) {
        log.fine("Grouping nodes into jobs.");

        // group Nodex by UUID
        // and search for job tasks
        final Map<UUID, List<HistoryRecord>> idGroups = new HashMap<>();
        final Set<UUID> jobIds = new HashSet<>();
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

        final List<Element> result = new ArrayList<>(jobIds.size());
        Element group;
        for (UUID id : jobIds) {
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
