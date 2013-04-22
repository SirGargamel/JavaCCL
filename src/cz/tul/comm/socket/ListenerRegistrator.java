package cz.tul.comm.socket;

import cz.tul.comm.communicator.DataPacket;
import cz.tul.comm.socket.queue.Identifiable;
import cz.tul.comm.socket.queue.Listener;
import java.util.Observer;
import java.util.Queue;
import java.util.UUID;

/**
 * Registrator of data listeners.
 *
 * @author Petr Ječmen
 */
public interface ListenerRegistrator {

    /**
     * Register listener for given IP.
     *
     * @param clientId UUID of target listener
     * @param dataListener target listener
     * @param wantsPushNotifications true if listeners wants to be notified that
     * message has arrived
     * @return Queue, which wil be used for data storing when data from this IP
     * is received.
     */
    Queue<DataPacket> addClientListener(final UUID clientId, final Listener dataListener, final boolean wantsPushNotifications);

    /**
     * Deregister given listener for given IP. If IP is null, then the listener
     * is removed completely (from all IPs).
     *
     * @param clientId UUID of target listener
     * @param dataListener target listener
     */
    void removeClientListener(final UUID clientId, final Listener dataListener);

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
    Queue<Identifiable> addIdListener(final Object id, final Listener idListener, final boolean wantsPushNotifications);

    /**
     * Deregister given listener for given ID. If ID is null, then the listener
     * is removed completely (from all IDs).
     *
     * @param id data ID for listening
     * @param idListener target listener
     */
    void removeIdListener(final Object id, final Listener idListener);

    /**
     * Register observer, that will receive all data received by this
     * registrator.
     *
     * @param msgObserver message observer
     */
    void addMessageObserver(final Observer msgObserver);

    /**
     * Deregister given observer.
     *
     * @param msgObserver message observer
     */
    void removeMessageObserver(final Observer msgObserver);
}
