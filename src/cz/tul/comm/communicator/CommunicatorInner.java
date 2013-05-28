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
    
     Queue<DataPacket> getUnsentData();

    void storeResponse(final DataPacket question, final Object response);
    
    /**
     * @param id UUID of target
     */
    void setTargetId(final UUID id);
    
    /**
     * @param id UUID of this communiator
     */
    void setSourceId(final UUID id);
    
    UUID getSourceId();
    
    void addObserver(final Observer o);
    
    void registerHistory(final HistoryManager hm);
    
}
