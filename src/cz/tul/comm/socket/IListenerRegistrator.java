package cz.tul.comm.socket;

import cz.tul.comm.socket.queue.IIdentifiable;
import cz.tul.comm.socket.queue.IListener;
import java.net.InetAddress;
import java.util.Observer;
import java.util.Queue;

/**
 *
 * @author Petr Ječmen
 */
public interface IListenerRegistrator {

    Queue<IPData> addIpListener(final InetAddress address, final IListener dataListener);

    void removeIpListener(final InetAddress address, final IListener dataListener);    

    Queue<IIdentifiable> addIdListener(final Object id, final IListener idListener);

    void removeIdListener(final Object id, final IListener idListener);   
    
    void registerMessageObserver(final Observer msgObserver);
    
    void deregisterMessageObserver(final Observer msgObserver);
}
