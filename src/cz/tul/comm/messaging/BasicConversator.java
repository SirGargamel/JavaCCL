package cz.tul.comm.messaging;

import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.socket.IListenerRegistrator;
import cz.tul.comm.socket.queue.IIdentifiable;
import cz.tul.comm.socket.queue.IListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class BasicConversator implements IListener {

    private static final Logger log = Logger.getLogger(BasicConversator.class.getName());
    private final Communicator target;
    private final IListenerRegistrator listener;
    private IIdentifiable receivedData;

    public BasicConversator(final Communicator target, final IListenerRegistrator listener) {
        this.target = target;
        this.listener = listener;
    }

    public IIdentifiable sendAndReceiveData(final IIdentifiable dataToSend) {
        receivedData = null;

        final Object id = dataToSend.getId();
        listener.addIdListener(id, this, true);
        target.sendData(dataToSend);

        synchronized (this) {
            try {
                this.wait();
            } catch (InterruptedException ex) {
                log.log(Level.WARNING, "Waiting of BasicConverstaro has been interrupted, result may not be actual data.", ex);
            }
        }

        listener.removeIdListener(id, this);
        return receivedData;
    }

    @Override
    public void receiveData(IIdentifiable data) {
        receivedData = data;
        synchronized (this) {
            this.notify();
        }
    }
}
