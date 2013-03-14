package cz.tul.comm.history.export;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Create XML representation of given data. Should return null for nonparsable
 * data.
 *
 * @author Petr Ječmen
 */
public interface IExportUnit {

    /**
     * Create XML representation of given object, IXMLExporter can help with
     * exporting unknown data or data, for which exporter already exists.
     *
     * @param doc target XML document.
     * @param data data for exporting
     * @return XML element representing given object.
     */
    Element exportData(final Document doc, final Object data);

    /**
     * @return Class which can be exported using this class.
     */
    Class<?> getExportedClass();
}
