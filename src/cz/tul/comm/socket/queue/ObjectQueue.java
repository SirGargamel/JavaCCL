package cz.tul.comm.socket.queue;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author Petr Ječmen
 */
public class ObjectQueue<O extends IIdentifiable> {

    private Map<Object, Map<IListener, Queue<O>>> data;

    public ObjectQueue() {
        data = new ConcurrentHashMap<>();
    }

    public Queue<O> getDataQueue(final Object id, final IListener owner) {
        Queue<O> result = null;

        Map<IListener, Queue<O>> m = data.get(id);
        if (m != null) {
            result = m.get(owner);
        }

        return result;
    }

    public Queue<O> registerListener(final Object id, final IListener owner) {
        Queue<O> result = new ConcurrentLinkedQueue<>();

        Map<IListener, Queue<O>> m = data.get(id);
        if (m == null) {
            m = new ConcurrentHashMap<>();
            data.put(id, m);
        }

        m.put(owner, result);

        return result;
    }

    public void deregisterListener(final Object id, final IListener owner) {
        Map<IListener, Queue<O>> m = data.get(id);
        if (m != null) {
            m.remove(owner);
        }
    }

    public void deregisterListener(final IListener owner) {
        for (Map<IListener, Queue<O>> m : data.values()) {
            m.remove(owner);
        }
    }

    public void storeData(final O data) {
        final Map<IListener, Queue<O>> m = this.data.get(data.getId());
        if (m != null) {
            for (Queue<O> q : m.values()) {
                q.add(data);
            }
        }
    }
}
