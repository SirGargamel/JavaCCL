package cz.tul.comm.socket.queue;

import cz.tul.comm.IService;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
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
class PushDaemon<O extends Identifiable> extends Thread implements IService {

    private static final Logger log = Logger.getLogger(PushDaemon.class.getName());
    private static final int TIME_WAIT = 1_000;
    private final Map<Object, Queue<O>> data;
    private final Map<Object, Listener> receivers;
    private final ExecutorService exec;
    private boolean run;

    PushDaemon(final Map<Object, Queue<O>> data) {
        this.data = data;
        receivers = new HashMap<>();
        exec = Executors.newCachedThreadPool();
        run = true;
    }

    void setPushReceiver(final Listener receiver, final Object id) {
        receivers.put(id, receiver);

        log.log(Level.FINE, "New push receiver for id {0} registered.", id.toString());
    }

    void removePushReceiver(final Object id) {
        receivers.remove(id);
        log.log(Level.FINE, "Push receiver for id {0} deregistered.", id.toString());


    }

    @Override
    public void run() {
        Queue<O> q;        
        Identifiable object;
        Object id;
        Listener l;
        while (run) {
            for (Entry<Object, Queue<O>> e : data.entrySet()) {
                id = e.getKey();
                if (receivers.containsKey(id)) {
                    l = receivers.get(id);
                    q = data.get(id);

                    if (q != null) {
                        while (!q.isEmpty()) {
                            object = q.poll();
                            log.log(Level.FINE, "Pushing data {0} to {1}", new Object[]{object.getId().toString(), l.toString()});
                            exec.execute(new Notifier(l, object));
                        }
                    }
                }
            }

            try {
                synchronized (this) {
                    this.wait();
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

        private final Listener listener;
        private final Identifiable data;

        Notifier(Listener listener, Identifiable data) {
            this.listener = listener;
            this.data = data;
        }

        @Override
        public void run() {
            listener.receiveData(data);
        }
    }
}
