package cz.tul.comm;

import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilities.
 *
 * @author Petr Ječmen
 */
public class Utils {

    private static final Logger log = Logger.getLogger(Utils.class.getName());
    private static final String LOG_FILE_NAME = "systemLog.xml";

    /**
     * enable logging of all actions
     */
    public static void enableDebugMode() {        
        adjustMainLoggerLevel(Level.FINE);
        adjustMainHandlersLoggingLevel(Level.FINE);
    }

    /**
     * Set level of main logger (and its children)
     *
     * @param level new Logger level
     */
    public static void adjustMainLoggerLevel(final Level level) {
        Logger l = log.getParent();
        l.setLevel(level);        
    }
    
    /**
     * Adjust level of logging (of Loggers) for a given class
     * @param cls target class
     * @param level new level of logging
     */
    public static void adjustClassLoggingLevel(final Class cls, final Level level) {
        Logger l = Logger.getLogger(cls.getName());
        l.setLevel(level);        
    }
    
    /**
     * Change level of logging of main Logger
     * @param level new logging level
     */
    public static void adjustMainHandlersLoggingLevel(final Level level) {
        Logger l = log.getParent();        
        for (Handler h : l.getHandlers()) {
            h.setLevel(level);
        }      
    }

    /**
     * initialize file logging
     */
    public static void useFileLogging() {
        try {
            Logger l = log.getParent();
            Handler fh = new FileHandler(LOG_FILE_NAME, true);
            if (l.getLevel() != null) {
                fh.setLevel(l.getLevel());
            } else {
                fh.setLevel(Level.ALL);
            }
            l.addHandler(fh);
        } catch (IOException | SecurityException ex) {
            log.log(Level.SEVERE, "Error preparing file logger.", ex);
        }
    }

    /**
     * Checks if the given object is fully serializable.
     *
     * @param data object for testing
     * @return true if object can be serialized using {@link ObjectOutputStream}
     */
    public static boolean checkSerialization(final Object data) {
        boolean result = false;

        if (data == null) {
            result = true;
        } else if (data instanceof Serializable || data instanceof Externalizable) {
            try (ObjectOutputStream o = new ObjectOutputStream(new ByteArrayOutputStream())) {
                o.writeObject(data);
                result = true;
            } catch (IOException ex) {
                // not serializable
            }
        }

        return result;
    }

    private Utils() {
    }
}
