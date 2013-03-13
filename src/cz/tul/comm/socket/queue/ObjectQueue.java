package cz.tul.comm.socket.queue;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author Petr Ječmen
 */
public class ObjectQueue<O extends IIdentifiable> {

    private final Map<Object, Map<IListener, Queue<O>>> data;    
    private final PushDaemon<O> pushDaemon;

    public ObjectQueue() {
        data = new ConcurrentHashMap<>();        
        pushDaemon = new PushDaemon<>(getDataQueues());        
    }

    public Queue<O> getDataQueue(final Object id, final IListener owner) {
        Queue<O> result = null;

        Map<IListener, Queue<O>> m = data.get(id);
        if (m != null) {
            result = m.get(owner);
        }

        return result;
    }

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

    public void deregisterListener(final Object id, final IListener owner) {
        Map<IListener, Queue<O>> m = data.get(id);
        if (m != null) {
            m.remove(owner);
        }
        pushDaemon.removePushReceiver(owner, id);
    }

    public void deregisterListener(final IListener owner) {
        for (Map<IListener, Queue<O>> m : data.values()) {
            m.remove(owner);
        }
        pushDaemon.removePushReceiver(owner, null);
    }

    public void storeData(final O data) {
        final Map<IListener, Queue<O>> m = this.data.get(data.getId());
        if (m != null) {
            for (Queue<O> q : m.values()) {
                q.add(data);
            }
        }
        if (!pushDaemon.isAlive()) {
            pushDaemon.start();
        }
    }

    private Collection<Map<IListener, Queue<O>>> getDataQueues() {
        return Collections.unmodifiableCollection(data.values());
    }
}
