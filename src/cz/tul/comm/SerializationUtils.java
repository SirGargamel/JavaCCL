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

    public static void saveItemToDisc(final File dest, final Object data, final boolean asXml) {
        if (data instanceof Serializable) {
            if (asXml) {
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
                    // TODO logging
                    Logger.getLogger(SerializationUtils.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(dest))) {
                    out.writeObject(data);
                } catch (IOException ex) {
                    // TODO logging
                    Logger.getLogger(SerializationUtils.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else {
            // TODO logging
            System.err.println("Object is not serializable.");
        }
    }

    public static Object loadItemFromDisc(final File src, final boolean isXml) {
        Object result = null;

        if (isXml) {
            try (XMLDecoder in = new XMLDecoder(new FileInputStream(src))) {
                result = in.readObject();
            } catch (FileNotFoundException ex) {
                // TODO logging
                Logger.getLogger(SerializationUtils.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(src))) {
                result = in.readObject();
            } catch (IOException | ClassNotFoundException ex) {
                // TODO logging
                Logger.getLogger(SerializationUtils.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return result;
    }
}
