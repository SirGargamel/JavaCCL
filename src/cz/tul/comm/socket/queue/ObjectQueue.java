package cz.tul.comm.socket.queue;

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

    private static final Logger log = Logger.getLogger(ObjectQueue.class.getName());
    private final Map<Object, Queue<O>> data;

    /**
     * Initialize new queue.
     */
    public ObjectQueue() {
        data = new ConcurrentHashMap<>();
    }

    /**
     * @param id data id
     * @param owner queue owner
     * @return data queue for given owner with data with given id
     */
    public Queue<O> getDataQueue(final Object id) {
        return data.get(id);
    }

    /**
     * @param id data ID
     * @param owner queue owner
     * @param wantsPush true if listener wants to be notified about new data
     * arrival
     * @return data queue, which will be used for storing data with given ID
     */
    public Queue<O> setListener(final Object id, final Listener<O> owner) {
        final Queue<O> result = new ConcurrentLinkedQueue<>();

        if (id != null) {
            data.put(owner, result);
            log.log(Level.FINE, "New listener registered - own: {1}, id:{0}", new Object[]{id.toString(), owner.toString()});
        }

        return result;
    }

    /**
     * Owner will not receive notifications about arrival of data with given ID
     *
     * @param id data ID
     * @param owner listener
     */
    public void removeListener(final Object id) {
        data.remove(id);
        log.log(Level.FINE, "Listener deregistered for id{0}", new Object[]{id.toString()});
    }

    /**
     * Store received data and alert listeners.
     *
     * @param data received data.
     */
    public void storeData(final O data) {
        if (data != null && data.getId() != null) {
            final Queue<O> q = this.data.get(data.getId());
            if (q != null) {
                q.add(data);
                log.log(Level.FINE, "Data [{0}] stored.", data.toString());
            }
        }
    }
}
