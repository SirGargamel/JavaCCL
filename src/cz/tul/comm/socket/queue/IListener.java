package cz.tul.comm.socket.queue;

/**
 *
 * @author Petr Ječmen
 */
public interface IListener<I, D> {
    
    void receiveData(final I id, final D data);
    
}
