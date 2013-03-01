package cz.tul.comm.socket.queue;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author Petr Ječmen
 */
public class ObjectQueue<I, O extends IListener<I, D>, D> {

    private Map<I, Map<O, Queue<D>>> data;

    public ObjectQueue() {
        data = new ConcurrentHashMap<>();
    }

    public Queue<D> getDataQueue(final I id, final O owner) {
        Queue<D> result = null;

        Map<O, Queue<D>> m = data.get(id);
        if (m != null) {
            result = m.get(owner);
        }

        return result;
    }

    public Queue<D> registerListener(final I id, final O owner) {
        Queue<D> result = new ConcurrentLinkedQueue<>();

        Map<O, Queue<D>> m = data.get(id);
        if (m == null) {
            m = new ConcurrentHashMap<>();
            data.put(id, m);
        }

        m.put(owner, result);

        return result;
    }

    public void deregisterListener(final I id, final O owner) {
        Map<O, Queue<D>> m = data.get(id);
        if (m != null) {
            m.remove(owner);
        }
    }
    
    public void deregisterListener(final O owner) {
        for (Map<O, Queue<D>> m : data.values()) {
            m.remove(owner);
        }
    }

    public void storeData(final I id, final D data) {
        Map<O, Queue<D>> m = this.data.get(id);
        if (m != null) {
            for (Queue<D> q : m.values()) {
                q.add(data);
            }
        }
    }
}
