package cz.tul.comm.socket.queue;

import cz.tul.comm.IService;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Queue for listeners to receive data from socket.
 *
 * @param <O> Data type of stored data, data must be identifiable via {!link
 * IIdentifiable} interface.
 * @author Petr Ječmen
 */
public class ObjectQueue<O extends IIdentifiable> implements IService {

    private final Map<Object, Map<IListener, Queue<O>>> data;
    private final PushDaemon<O> pushDaemon;

    /**
     * Initialize new queue.
     */
    public ObjectQueue() {
        data = new ConcurrentHashMap<>();
        pushDaemon = new PushDaemon<>(getDataQueues());
    }

    /**
     * @param id data id
     * @param owner queue owner
     * @return data queue for given owner with data with given id
     */
    public Queue<O> getDataQueue(final Object id, final IListener owner) {
        Queue<O> result = null;

        Map<IListener, Queue<O>> m = data.get(id);
        if (m != null) {
            result = m.get(owner);
        }

        return result;
    }

    /**
     * @param id data ID
     * @param owner queue owner
     * @param wantsPush true if listener wants to be notified about new data
     * arrival
     * @return data queue, which will be used for storing data with given ID
     */
    public Queue<O> registerListener(final Object id, final IListener owner, final boolean wantsPush) {
        Queue<O> result = new ConcurrentLinkedQueue<>();

        Map<IListener, Queue<O>> m = data.get(id);
        if (m == null) {
            m = new ConcurrentHashMap<>();
            data.put(id, m);
        }

        m.put(owner, result);

        if (wantsPush) {
            pushDaemon.addPushReceiver(owner, id);
        }

        return result;
    }

    /**
     * Owner will not receive notifications about arrival of data with given ID
     *
     * @param id data ID
     * @param owner listener
     */
    public void deregisterListener(final Object id, final IListener owner) {
        Map<IListener, Queue<O>> m = data.get(id);
        if (m != null) {
            m.remove(owner);
        }
        pushDaemon.removePushReceiver(owner, id);
    }

    /**
     * Owner will not receive aby notifications about incoming data.
     *
     * @param owner listener
     */
    public void deregisterListener(final IListener owner) {
        for (Map<IListener, Queue<O>> m : data.values()) {
            m.remove(owner);
        }
        pushDaemon.removePushReceiver(owner, null);
    }

    /**
     * Store received data and alert listeners.
     *
     * @param data received data.
     */
    public void storeData(final O data) {
        final Map<IListener, Queue<O>> m = this.data.get(data.getId());
        if (m != null) {
            for (Queue<O> q : m.values()) {
                q.add(data);
            }
        }
        if (!pushDaemon.isAlive()) {
            pushDaemon.start();
        } else {
            synchronized (pushDaemon) {
                pushDaemon.notify();
            }
        }
    }

    private Collection<Map<IListener, Queue<O>>> getDataQueues() {
        return Collections.unmodifiableCollection(data.values());
    }

    @Override
    public void stopService() {
        pushDaemon.stopService();
    }
}
