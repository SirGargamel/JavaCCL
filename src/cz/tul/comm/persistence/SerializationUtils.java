package cz.tul.comm.persistence;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilities for object persistence.
 *
 * @author Petr Ječmen
 */
public abstract class SerializationUtils {

    private static final Logger log = Logger.getLogger(SerializationUtils.class.getName());

    /**
     *
     * @param dest target file
     * @param data object for saving
     * @return true for successfull saving
     * @throws IOException target file could not be accessed
     */
    public static boolean saveItemToDisc(final File dest, final Object data) throws IOException {
        log.log(Level.FINER, "Saving item to disk as binary - {0}, {1}", new Object[]{dest.getName(), data.toString()});

        boolean result = false;
        if (data instanceof Serializable) {
            if (!dest.exists()) {
                try {
                    dest.createNewFile();
                } catch (IOException ex) {
                    log.log(Level.WARNING, "Target file for serialization could not be created.", ex);
                }
            }
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(dest))) {
                out.writeObject(data);
                result = true;
            } catch (IOException ex) {
                throw new IOException(ex);
            }
        } else {
            log.warning("Object is not serializable.");
        }
        return result;
    }

    /**
     *
     * @param dest target file
     * @param data object for saving
     * @return true for successfull saving
     */
    public static boolean saveItemToDiscAsXML(final File dest, final Object data) {
        log.log(Level.FINER, "Saving item to disk as XML - {0}, {1}", new Object[]{dest.getName(), data.toString()});

        boolean result = false;
        if (data instanceof Serializable) {
            if (!dest.exists()) {
                try {
                    dest.createNewFile();
                } catch (IOException ex) {
                    log.log(Level.WARNING, "Target file for serialization could not be created.", ex);
                }
            }
            try (XMLEncoder out = new XMLEncoder(new FileOutputStream(dest))) {
                out.writeObject(data);
                result = true;
            } catch (FileNotFoundException ex) {
                log.log(Level.WARNING, "Target file for serialization is inaccessible.", ex);
            }
        } else {
            log.warning("Object is not serializable.");
        }
        return result;
    }

    /**
     * Load object from file created by ObjectOutputStream
     *
     * @param src source file
     * @return deserialized object
     */
    public static Object loadItemFromDisc(final File src) {
        log.log(Level.FINER, "Loading binary item from disk - {0}", new Object[]{src.getName()});
        Object result = null;

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(src))) {
            result = in.readObject();
        } catch (IOException ex) {
            log.log(Level.WARNING, "Input file for deserialization is inaccessible.", ex);
        } catch (ClassNotFoundException ex) {
            log.log(Level.WARNING, "Source file contains unsupported data.", ex);
        }

        return result;
    }

    /**
     * Load object from file created by XMLEncoder
     *
     * @param src source file
     * @return deserialized object
     */
    public static Object loadXMLItemFromDisc(final File src) {
        log.log(Level.FINER, "Loading XML item from disk - {0}", new Object[]{src.getName()});
        Object result = null;

        try (XMLDecoder in = new XMLDecoder(new FileInputStream(src))) {
            result = in.readObject();
        } catch (FileNotFoundException ex) {
            log.log(Level.WARNING, "Input file for deserialization is inaccessible.", ex);
        }

        return result;
    }
}
