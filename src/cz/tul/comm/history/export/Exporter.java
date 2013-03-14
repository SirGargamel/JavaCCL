package cz.tul.comm.history.export;

import java.util.HashMap;
import java.util.Map;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Class for exporting history to XML file.
 *
 * @author Petr Ječmen
 */
public abstract class Exporter {

    private static final Map<Class<?>, IExportUnit> exporters;

    static {
        exporters = new HashMap<>();

        registerExporterUnit(new ExportMessage());
    }

    /**
     * Create XML representation of given object
     *
     * @param data object for export
     * @param doc target XML document
     * @return XML node containing given objects data
     */
    public static Element exportObject(Object data, Document doc) {
        Element result;

        Class<?> c = data.getClass();
        if (exporters.containsKey(c)) {
            result = exporters.get(c).exportData(doc, data);
        } else {
            result = doc.createElement(data.getClass().getSimpleName());
            result.appendChild(doc.createTextNode(data.toString()));
        }

        return result;
    }

    /**
     * Register new exporting unit.
     *
     * @param eu new exporter class
     */
    public static void registerExporterUnit(final IExportUnit eu) {
        exporters.put(eu.getExportedClass(), eu);
    }
}
