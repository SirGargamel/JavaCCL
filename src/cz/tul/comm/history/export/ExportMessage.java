package cz.tul.comm.history.export;

import cz.tul.comm.history.History;
import cz.tul.comm.history.IXMLExporter;
import cz.tul.comm.messaging.Message;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author Petr Ječmen
 */
public class ExportMessage implements IExportUnit {

    @Override
    public Element exportData(Document doc, Object data, final IXMLExporter exp) {
        Element result = null;
        if (data instanceof Message) {
            final Message m = (Message) data;
            result = doc.createElement("Message");

            appendStringDataToNode(result, doc, "UUID", m.getId().toString());
            appendStringDataToNode(result, doc, "Header", m.getHeader());
            result.appendChild(exp.exportObject(m.getData(), doc));
        }

        return result;
    }

    @Override
    public Class<?> getExportedClass() {
        return Message.class;
    }

    private static void appendStringDataToNode(final Node n, final Document d, final String name, final String data) {
        final Element e = d.createElement(name);
        e.appendChild(d.createTextNode(data));
        n.appendChild(e);
    }
}
