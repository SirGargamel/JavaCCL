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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Extracts messages and groups them according to their IDs.
 *
 * @author Petr Ječmen
 */
public class UUIDSorter extends HistorySorter {

    private static final Logger log = Logger.getLogger(UUIDSorter.class.getName());
    private static final String FIELD_NAME_UUID = "UUID";

    @Override
    public List<Element> sortHistory(final Collection<HistoryRecord> records, final Document doc) {
        log.fine("Sotring messages by UUID.");

        // group Nodes by UUID        
        final SortedMap<UUID, List<HistoryRecord>> idGroups = new TreeMap<UUID, List<HistoryRecord>>();
        List<HistoryRecord> l;        
        Element o;
        NodeList nl;
        Node uuidNode;
        UUID uuid;
        for (HistoryRecord r : records) {
            o = r.getData();
            nl = o.getElementsByTagName(FIELD_NAME_UUID);
            if (nl.getLength() > 0) {
                uuidNode = nl.item(0);
                uuid = UUID.fromString(uuidNode.getFirstChild().getTextContent());
                l = idGroups.get(uuid);
                if (l == null) {
                    l = new ArrayList<HistoryRecord>();
                    idGroups.put(uuid, l);
                }
                l.add(r);
            }
        }

        final List<Element> result = new ArrayList<Element>(records.size());
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
