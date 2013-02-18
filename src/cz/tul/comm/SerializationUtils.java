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
 *
 * @author Petr Ječmen
 */
public abstract class SerializationUtils implements Serializable {

    private static final Logger log = Logger.getLogger(SerializationUtils.class.getName());

    public static void saveItemToDisc(final File dest, final Object data) throws IOException {
        if (data instanceof Serializable) {
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(dest))) {
                out.writeObject(data);
            } catch (IOException ex) {
                throw new IOException(ex);
            }
        } else {
            log.warning("Object is not serializable.");
        }
    }

    public static void saveItemToDiscAsXML(final File dest, final Object data) {
        if (dest.exists() && data instanceof Serializable) {
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
            } catch (FileNotFoundException ex) {
                log.log(Level.WARNING, "Target file for serialization is inaccessible.", ex);
            }
        } else {
            log.warning("Object is not serializable.");
        }
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
