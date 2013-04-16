package cz.tul.comm;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class Utils {

    private static final Logger log = Logger.getLogger(Utils.class.getName());
    private static final String LOG_FILE_NAME = "systemLog.xml";

    public static void adjustMainLoggerLevel(final Level level) {
        Logger l = Logger.getGlobal();
        l.setLevel(level);
        for (Handler h : l.getHandlers()) {
            h.setLevel(level);
        }
    }

    public static void prepareFileLogger() {
        try {
            Logger l = Logger.getGlobal();
            Handler fh = new FileHandler(LOG_FILE_NAME, true);
            if (l.getLevel() != null) {
                fh.setLevel(l.getLevel());
            }
            l.addHandler(fh);
        } catch (IOException | SecurityException ex) {
            log.log(Level.SEVERE, "Error preparing file logger.", ex);
        }
    }
}
