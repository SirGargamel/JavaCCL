package cz.tul.comm.communicator;

import cz.tul.comm.history.HistoryManager;
import java.util.Observer;
import java.util.Queue;
import java.util.UUID;

/**
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
     * @param id UUID of this communiator
     */
    void setSourceId(final UUID id);

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
