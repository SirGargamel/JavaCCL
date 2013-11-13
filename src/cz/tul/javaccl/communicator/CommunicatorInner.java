package cz.tul.javaccl.communicator;

import cz.tul.javaccl.history.HistoryManager;
import java.util.Observer;
import java.util.Queue;
import java.util.UUID;

/**
 * Extension to {@link Communicator} interface used only inside the library.
 *
 * @author Petr Ječmen
 */
public interface CommunicatorInner extends Communicator {

    /**
     * @return queue with unsent data packets
     */
    Queue<DataPacket> getUnsentData();

    /**
     * Store response.
     *
     * @param question data packet with question
     * @param response response to question
     */
    void storeResponse(final DataPacket question, final Object response);

    /**
     * @param id UUID of target
     */
    void setTargetId(final UUID id);

    /**
     * @return local UUID
     */
    UUID getSourceId();

    /**
     * Add new observer.
     *
     * @param o new observer
     */
    void addObserver(final Observer o);

    /**
     * Register new history manager.
     *
     * @param hm new history manager
     */
    void registerHistory(final HistoryManager hm);
}
