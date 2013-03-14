package cz.tul.comm.socket.queue;

/**
 * Interface for data receiving for registered listeners.
 *
 * @author Petr Ječmen
 */
public interface IListener {

    /**
     * Notification for listener that data with registered id has been received.
     *
     * @param data received data
     */
    void receiveData(final IIdentifiable data);
}
