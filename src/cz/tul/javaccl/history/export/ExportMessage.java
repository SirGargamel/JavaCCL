package cz.tul.javaccl.history.export;

import cz.tul.javaccl.messaging.Message;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Export unit for class {@link Message}. Stores 3 nodes - UUID, header and
 * data. Data are exported using {@link Exporter} and its methods, so if you
 * want to have nice export for sent data, create and register export unit for
 * given data.
 *
 * @author Petr Ječmen
 */
public class ExportMessage implements ExportUnit {

    private static void appendStringDataToNode(final Node n, final Document d, final String name, final String data) {
        final Element e = d.createElement(name);
        e.appendChild(d.createTextNode(data));
        n.appendChild(e);
    }

    @Override
    public Element exportData(Document doc, Object data) {
        Element result = null;
        if (data instanceof Message) {
            final Message m = (Message) data;
            result = doc.createElement("Message");

            appendStringDataToNode(result, doc, "UUID", m.getId().toString());
            appendStringDataToNode(result, doc, "Header", m.getHeader());
            if (m.getData() != null) {
                result.appendChild(Exporter.exportObject(m.getData(), doc));
            }
        }

        return result;
    }

    @Override
    public Class<?> getExportedClass() {
        return Message.class;
    }
}
