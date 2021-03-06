package cz.tul.javaccl.socket;

import cz.tul.javaccl.messaging.Identifiable;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Queue for listeners to receive data from socket.
 *
 * @param <O> Data type of stored data, data must be identifiable via {!link
 * Identifiable} interface.
 * @author Petr Ječmen
 */
public class ObjectQueue<O extends Identifiable> {

    private static final Logger LOG = Logger.getLogger(ObjectQueue.class.getName());
    private final Map<Object, Queue<O>> data;

    /**
     * Initialize new queue.
     */
    public ObjectQueue() {
        data = new ConcurrentHashMap<Object, Queue<O>>();
    }

    /**
     * @param id data id
     * @return data queue for given owner with data with given id
     */
    public Queue<O> getDataQueue(final Object id) {
        return data.get(id);
    }

    /**
     * @param id data ID
     * @return data queue, which will be used for storing data with given ID
     */
    public Queue<O> prepareQueue(final Object id) {
        final Queue<O> result = new ConcurrentLinkedQueue<O>();

        if (id != null) {
            data.put(id, result);
            LOG.log(Level.FINE, "New queue prepared for id " + id.toString());
        }

        return result;
    }

    /**
     * Check if for the given ID there is a listener registered.
     *
     * @param id message ID
     * @return true if there is a listener registered
     */
    public boolean isListenerRegistered(final Object id) {
        return data.containsKey(id);
    }

    /**
     * Owner will not receive notifications about arrival of data with given ID
     *
     * @param id data ID
     */
    public void removeListener(final Object id) {
        data.remove(id);
        LOG.log(Level.FINE, "Listener deregistered for id " + id.toString());
    }

    /**
     * Store received data.
     *
     * @param id ID of the data
     * @param data data for storing
     */
    public void storeData(final Object id, final O data) {
        if (data != null && data.getId() != null) {
            final Queue<O> q = this.data.get(id);
            if (q != null) {
                q.add(data);
                LOG.log(Level.FINE, "Data [" + data.toString() + "] stored.");
            }
        }
    }

    /**
     * Store received data.
     *
     * @param data data for storing.
     */
    public void storeData(final O data) {
        if (data != null) {
            storeData(data.getId(), data);
        }
    }
}
