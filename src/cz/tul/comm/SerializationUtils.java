package cz.tul.comm;

import java.beans.DefaultPersistenceDelegate;
import java.beans.Encoder;
import java.beans.Expression;
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
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilities for object persistence.
 *
 * @author Petr Ječmen
 */
public abstract class SerializationUtils {

    private static final Logger log = Logger.getLogger(SerializationUtils.class.getName());

    public static boolean saveItemToDisc(final File dest, final Object data) throws IOException {
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

    public static boolean saveItemToDiscAsXML(final File dest, final Object data) {
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
                out.setPersistenceDelegate(Inet4Address.class, new DefaultPersistenceDelegate() {
                    @Override
                    protected Expression instantiate(Object oldInstance, Encoder out) {
                        InetAddress old = (InetAddress) oldInstance;
                        return new Expression(oldInstance, InetAddress.class, "getByAddress",
                                new Object[]{old.getAddress()});
                    }
                });
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

    public static Object loadItemFromDisc(final File src) {
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

    public static Object loadXMLItemFromDisc(final File src) {
        Object result = null;

        try (XMLDecoder in = new XMLDecoder(new FileInputStream(src))) {
            result = in.readObject();
        } catch (FileNotFoundException ex) {
            log.log(Level.WARNING, "Input file for deserialization is inaccessible.", ex);
        }

        return result;
    }
}
