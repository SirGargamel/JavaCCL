package cz.tul.comm.socket;

import cz.tul.comm.socket.queue.IIdentifiable;
import cz.tul.comm.socket.queue.IListener;
import java.net.InetAddress;
import java.util.Observer;
import java.util.Queue;

/**
 * Registrator of data listeners.
 *
 * @author Petr Ječmen
 */
public interface IListenerRegistrator {

    /**
     * Register listener for given IP.
     *
     * @param address IP address for listening
     * @param dataListener target listener
     * @param wantsPushNotifications true if listeners wants to be notified that
     * message has arrived
     * @return Queue, which wil be used for data storing when data from this IP
     * is received.
     */
    Queue<IPData> addIpListener(final InetAddress address, final IListener dataListener, final boolean wantsPushNotifications);

    /**
     * Deregister given listener for given IP. If IP is null, then the listener
     * is removed completely (from all IPs).
     *
     * @param address IP address for listening
     * @param dataListener target listener
     */
    void removeIpListener(final InetAddress address, final IListener dataListener);

    /**
     * Register listener for given ID.
     *
     * @param id data ID for listening
     * @param idListener target listener
     * @param wantsPushNotifications true if listeners wants to be notified that
     * message has arrived
     * @return Queue, which wil be used for data storing when data with this ID
     * is received.
     */
    Queue<IIdentifiable> addIdListener(final Object id, final IListener idListener, final boolean wantsPushNotifications);

    /**
     * Deregister given listener for given ID. If ID is null, then the listener
     * is removed completely (from all IDs).
     *
     * @param id data ID for listening
     * @param idListener target listener
     */
    void removeIdListener(final Object id, final IListener idListener);

    /**
     * Register observer, that will receive all data received by this
     * registrator.
     *
     * @param msgObserver message observer
     */
    void registerMessageObserver(final Observer msgObserver);

    /**
     * Deregister given observer.
     *
     * @param msgObserver message observer
     */
    void deregisterMessageObserver(final Observer msgObserver);
}
