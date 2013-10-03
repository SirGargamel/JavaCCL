package cz.tul.javaccl.history.export;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Class for exporting history to XML file.
 *
 * @author Petr Ječmen
 */
public abstract class Exporter {

    private static final Logger log = Logger.getLogger(Exporter.class.getName());
    private static final Map<Class<?>, ExportUnit> exporters;

    static {
        exporters = new HashMap<Class<?>, ExportUnit>();

        registerExporterUnit(new ExportMessage());
    }

    /**
     * Create XML representation of given object using registered exporter
     * units.
     *
     * @param data object for export
     * @param doc target XML document
     * @return XML node containing given objects data
     */
    public static Element exportObject(Object data, Document doc) {
        Element result;

        Class<?> c = data.getClass();
        if (exporters.containsKey(c)) {
            log.log(Level.CONFIG, "Exporting object using {0} exporter.", c.getName());
            result = exporters.get(c).exportData(doc, data);
        } else {
            log.log(Level.CONFIG, "Exporting object using default exporter.");
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
    public static void registerExporterUnit(final ExportUnit eu) {
        exporters.put(eu.getExportedClass(), eu);
        log.log(Level.FINE, "New exporter for class {0} registered.", eu.getExportedClass().getName());
    }

    private Exporter() {
    }
}
