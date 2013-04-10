package cz.tul.comm.socket.queue;

import cz.tul.comm.IService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
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
class PushDaemon<O extends IIdentifiable> extends Thread implements IService {

    private static final Logger log = Logger.getLogger(PushDaemon.class.getName());
    private static final int TIME_WAIT = 1_000;
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

        log.log(Level.FINE, "New push receiver for id {0} registered.", id.toString());
    }

    void removePushReceiver(final IListener receiver, final Object id) {
        if (id == null) {
            receivers.remove(receiver);
            log.log(Level.FINE, "Push receiver {1} deregistered.", receiver.toString());
        } else {
            final List<Object> l = receivers.get(receiver);
            if (l != null) {
                l.remove(id);
                log.log(Level.FINE, "Push receiver for id {0} deregistered.", id.toString());
            }
        }
    }

    @Override
    public void run() {
        Queue<O> q;
        Queue<O> tmp = new LinkedList<>();
        IIdentifiable object;
        while (run) {
            for (Map<IListener, Queue<O>> m : data) {
                for (IListener l : m.keySet()) {
                    if (receivers.containsKey(l)) {
                        tmp.clear();
                        q = m.get(l);

                        while (!q.isEmpty()) {
                            object = q.poll();
                            if (receivers.get(l).contains(object.getId())) {
                                log.log(Level.CONFIG, "Pushing data {0} to {1}", new Object[]{object.getId().toString(), l.toString()});
                                exec.execute(new Notifier(l, object));
                            } else {
                                tmp.add((O) object);
                                log.log(Level.CONFIG, "Data {0} not pushed to {1} because he is not registered for pushing objects with this ID.", new Object[]{object.getId().toString(), l.toString()});
                            }
                        }

                        // store back data that havent been pushed
                        q.addAll(tmp);
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
        exec.shutdownNow();
        log.fine("PushDameon has been stopped.");
    }

    private static class Notifier implements Runnable {

        private final IListener listener;
        private final IIdentifiable data;

        Notifier(IListener listener, IIdentifiable data) {
            this.listener = listener;
            this.data = data;
        }

        @Override
        public void run() {
            listener.receiveData(data);
        }
    }
}
