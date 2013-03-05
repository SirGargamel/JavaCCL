package cz.tul.comm.socket.queue;

/**
 *
 * @author Petr Ječmen
 */
public interface IListener {
    
    void receiveData(final IIdentifiable data);
    
}
