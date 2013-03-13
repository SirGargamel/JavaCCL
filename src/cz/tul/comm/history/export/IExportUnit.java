package cz.tul.comm.history.export;

import cz.tul.comm.history.IXMLExporter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Create XML representation of given data. Should return null for nonparsable
 * data.
 *
 * @author Petr Ječmen
 */
public interface IExportUnit {

    Element exportData(final Document doc, final Object data, final IXMLExporter exp);
    
    Class<?> getExportedClass();
}
