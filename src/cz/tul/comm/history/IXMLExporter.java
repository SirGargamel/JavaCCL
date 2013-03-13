package cz.tul.comm.history;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author Petr Ječmen
 */
public interface IXMLExporter {

    Element exportObject(final Object data, final Document doc);
}
