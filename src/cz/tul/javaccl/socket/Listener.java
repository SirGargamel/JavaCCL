package cz.tul.javaccl.socket;

import cz.tul.javaccl.messaging.Identifiable;

/**
 * Interface for data receiving for registered listeners.
 *
 * @param <T> datatype of received messages, need to provide method for ID
 * determination
 * @author Petr Ječmen
 */
public interface Listener<T extends Identifiable> {

    /**
     * Notification for listener that data with registered id has been received.
     *
     * @param data received data
     * @return response to the message
     */
    Object receiveData(final T data);
}
