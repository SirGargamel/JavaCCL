package cz.tul.javaccl.persistence;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Lenam s.r.o.
 */
public final class Timeout {

    private static final Logger LOG = Logger.getLogger(Timeout.class.getName());
    private static final Properties TIMES;

    static {
        TIMES = new Properties();
        InputStream in = null;
        try {
            in = Timeout.class.getResourceAsStream("timeouts.properties");
            TIMES.load(in);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Error reading default timeout settings.", ex);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    LOG.log(Level.WARNING, "Error closing properties reader.", ex);
                }
            }
        }
    }

    /**
     * Obtain desired timeout
     *
     * @param type type of timeout
     * @return numerical value of timeout in miliseconds
     */
    public static int getTimeout(final TimeoutType type) {
        return Integer.parseInt(TIMES.get(type.toString()).toString());
    }

    /**
     * Load custom timeouts from properties file. Used values are in
     * {@link cz.tul.javaccl.persistenceTimeoutType} enum.
     *
     * @param in input stream with properties file
     * @throws IOException error reading input file
     */
    public static void loadTimeoutProperties(final FileInputStream in) throws IOException {
        TIMES.load(in);
    }

    /**
     * Enumeration of used timeouts
     */
    public static enum TimeoutType {

        /**
         *
         */
        MESSAGE,
        /**
         *
         */
        DISCOVERY,
        /**
         *
         */
        JOB_CHECK,;
    }

    private Timeout() {
    }
}
