package cz.tul.comm.socket.queue;

import cz.tul.comm.IService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Daemon alerting listeners that they have unhandled data in queues.
 *
 * @author Petr Ječmen
 */
final class PushDaemon<O extends IIdentifiable> extends Thread implements IService {

    private static final Logger log = Logger.getLogger(PushDaemon.class.getName());
    private static final int TIME_WAIT = 1000;
    private final Collection<Map<IListener, Queue<O>>> data;
    private final Map<IListener, List<Object>> receivers;
    private final ExecutorService exec;
    private boolean run;

    PushDaemon(final Collection<Map<IListener, Queue<O>>> data) {
        this.data = data;
        receivers = new HashMap<>();
        exec = Executors.newCachedThreadPool();
        run = true;
    }

    void addPushReceiver(final IListener receiver, final Object id) {
        List<Object> l = receivers.get(receiver);
        if (l == null) {
            l = new ArrayList<>();
            receivers.put(receiver, l);
        }
        l.add(id);
    }

    void removePushReceiver(final IListener receiver, final Object id) {
        if (id == null) {
            receivers.remove(receiver);
        } else {
            final List<Object> l = receivers.get(receiver);
            if (l != null) {
                l.remove(id);
            }
        }
    }

    @Override
    public void run() {
        Queue<O> q;
        IIdentifiable object;
        while (run) {
            for (Map<IListener, Queue<O>> m : data) {
                for (IListener l : m.keySet()) {
                    if (receivers.containsKey(l)) {
                        q = m.get(l);

                        while (!q.isEmpty()) {
                            object = q.poll();
                            if (receivers.get(l).contains(object.getId())) {
                                exec.execute(new Notifier(l, object));
                            }
                        }
                    }
                }
            }

            try {
                synchronized (this) {
                    this.wait(TIME_WAIT);
                }
            } catch (InterruptedException ex) {
                log.log(Level.WARNING, "PushDameon pause has been interrupted.", ex);
            }

        }
    }

    @Override
    public void stopService() {
        run = false;
    }

    private static class Notifier implements Runnable {

        private final IListener listener;
        private final IIdentifiable data;

        public Notifier(IListener listener, IIdentifiable data) {
            this.listener = listener;
            this.data = data;
        }

        @Override
        public void run() {
            listener.receiveData(data);
        }
    }
}