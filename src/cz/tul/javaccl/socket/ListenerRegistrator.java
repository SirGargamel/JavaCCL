package cz.tul.javaccl.socket;

import cz.tul.javaccl.communicator.DataPacket;
import cz.tul.javaccl.messaging.Identifiable;
import java.util.Observer;
import java.util.Queue;
import java.util.UUID;

/**
 * Registrator of data listeners. Both push and pull variants available, but
 * only the push variant offers a possibility to directly reply to received
 * message.
 *
 * @author Petr Ječmen
 */
public interface ListenerRegistrator {

    /**
     * Register listener for given IP.
     *
     * @param clientId UUID of target listener
     * @param dataListener target listener
     */
    void setClientListener(final UUID clientId, final Listener<DataPacket> dataListener);

    /**
     * Retrieve a queue with messages received from given client.
     *
     * @param clientId UUID of the client
     * @return message queue
     */
    Queue<DataPacket> createClientMessageQueue(final UUID clientId);

    /**
     * Deregister a client listener.
     *
     * @param clientId UUID of the client
     */
    void removeClientListener(final UUID clientId);

    /**
     * Register listener for given ID.
     *
     * @param id data ID for listening
     * @param idListener target listener
     */
    void setIdListener(final Object id, final Listener<Identifiable> idListener);

    /**
     * Retrieve a queue with received messages with given ID.
     *
     * @param id message ID
     * @return message queue
     */
    Queue<Identifiable> createIdMessageQueue(final Object id);

    /**
     * Deregister given listener for given ID. If ID is null, then the listener
     * is removed completely (from all IDs).
     *
     * @param id data ID for listening
     */
    void removeIdListener(final Object id);

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

    /**
     * Assigns new message listener which will receive all incoming messages as
     * DataPackets with possibility to reply to them.
     *
     * @param listener new message listener
     */
    void setMessageListener(final Listener<DataPacket> listener);

    /**
     * deregisters current message listener
     */
    void removeMessageListener();

    /**
     * @return queue that contains all incoming messages
     */
    Queue<DataPacket> getMessageQueue();
}
