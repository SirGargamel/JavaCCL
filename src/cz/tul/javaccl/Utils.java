package cz.tul.javaccl;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.XMLFormatter;

/**
 * Utilities.
 *
 * @author Petr Ječmen
 */
public class Utils {

    private static final Logger LOG = Logger.getLogger(Utils.class.getName());
    private static final String LOG_FILE_NAME_S = "systemLogServer.xml";
    private static final String LOG_FILE_NAME_C = "systemLogClient.xml";

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
        Logger l = Logger.getLogger("");
        l.setLevel(level);
    }

    /**
     * Adjust level of logging (of Loggers) for a given class
     *
     * @param cls target class
     * @param level new level of logging
     */
    public static void adjustClassLoggingLevel(final Class cls, final Level level) {
        Logger l = Logger.getLogger(cls.getName());
        l.setLevel(level);
    }

    /**
     * Change level of logging of main Logger
     *
     * @param level new logging level
     */
    public static void adjustMainHandlersLoggingLevel(final Level level) {
        Logger l = Logger.getLogger("");
        for (Handler h : l.getHandlers()) {
            h.setLevel(level);
        }
    }

    /**
     * Initialize console logging
     */
    public static void initConsoleLogging() {
        try {
            final Handler console = new ConsoleHandler();
            console.setFormatter(new SimpleFormatter());
            console.setLevel(Level.INFO);

            final Logger l = Logger.getLogger("");
            l.setLevel(Level.FINE);

            boolean addConsole = true;
            for (Handler h : l.getHandlers()) {
                if (h instanceof ConsoleHandler) {
                    addConsole = false;
                    break;
                }
            }
            if (addConsole) {
                l.addHandler(console);
            }
        } catch (SecurityException ex) {
            LOG.log(Level.SEVERE, "Error preparing file logger.", ex);
            LOG.log(Level.FINE, "Error preparing file logger.", ex);
        }
    }

    /**
     * Creates a LOG file and stores all logging info in this file. Server and
     * client have separate LOG files.
     *
     * @param isServer to determine which file to use for logging
     */
    public static void enableFileLogging(final boolean isServer) {
        try {
            final Handler fh;
            if (isServer) {
                fh = new FileHandler(LOG_FILE_NAME_S, true);
            } else {
                fh = new FileHandler(LOG_FILE_NAME_C, true);
            }
            fh.setFormatter(new XMLFormatter());
            fh.setLevel(Level.FINE);

            final Logger l = Logger.getLogger("");
            l.setLevel(Level.FINE);
            l.addHandler(fh);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Error preparing file logger.");
            LOG.log(Level.FINE, "Error preparing file logger.", ex);
        } catch (SecurityException ex) {
            LOG.log(Level.SEVERE, "Error preparing file logger.", ex);
            LOG.log(Level.FINE, "Error preparing file logger.", ex);
        }
    }

    private Utils() {
    }
}
