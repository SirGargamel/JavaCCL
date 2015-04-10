package cz.tul.javaccl.history.export;

import java.util.Arrays;
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
public class Exporter extends ExportUnit {

    private static final Logger LOG = Logger.getLogger(Exporter.class.getName());
    private static final String ARRAY_NAME = "Array";
    private static final Map<Class<?>, ExportUnit> EXPORTERS;

    static {
        EXPORTERS = new HashMap<Class<?>, ExportUnit>();
    }

    /**
     * Create XML representation of given object using registered exporter
     * units.
     *
     * @param data object for export
     * @param doc target XML document
     * @return XML node containing given objects data
     */
    @Override
    public Element exportData(final Document doc, final Object data) {
        Element result;
        if (data == null) {
            result = doc.createElement("null");
        } else {
            final Class<?> cls = data.getClass();
            if (EXPORTERS.containsKey(cls)) {
                result = EXPORTERS.get(cls).exportData(doc, data);
            } else {
                if (data.getClass().isArray()) {
                    result = doc.createElement(ARRAY_NAME);
                    result.appendChild(doc.createTextNode(Arrays.toString((Object[]) data)));
                } else {
                    result = doc.createElement(data.getClass().getSimpleName());
                    result.appendChild(doc.createTextNode(data.toString()));
                }
            }
        }

        return result;
    }

    /**
     * Register new exporting unit.
     *
     * @param unit new exporter class
     */
    public static void registerExporterUnit(final ExportUnit unit) {
        EXPORTERS.put(unit.getExportedClass(), unit);
        LOG.log(Level.FINE, "New exporter for class " + unit.getExportedClass().getName() + " registered.");
    }

    @Override
    public Class<?> getExportedClass() {
        // not general class to export, holds map of other exporters
        return Exporter.class;
    }
}
