package cz.tul.comm.history;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Interface for exporting object to XML.
 *
 * @author Petr Ječmen
 */
public interface IXMLExporter {

    /**
     * Create XML representation of given object (use {@link IExportUnit}s for
     * known classes and general mechanism for uknown classes).
     *
     * @param data data for export.
     * @param doc target XML document.
     * @return XML representatin of given object.
     */
    Element exportObject(final Object data, final Document doc);
}
