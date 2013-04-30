package cz.tul.comm.messaging;

import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.socket.ListenerRegistrator;
import cz.tul.comm.socket.queue.Identifiable;
import cz.tul.comm.socket.queue.Listener;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class offering basic conversation scheme request-reply with blocking method.
 * @author Petr Ječmen
 */
public class BasicConversator implements Listener {

    private static final Logger log = Logger.getLogger(BasicConversator.class.getName());
    private final Communicator target;
    private final ListenerRegistrator listener;
    private Identifiable receivedData;

    /**
     * Prepare new instance of converstaor.
     *
     * @param target communication target
     * @param listener class, which is receiving messages
     */
    public BasicConversator(final Communicator target, final ListenerRegistrator listener) {
        this.target = target;
        this.listener = listener;
    }

    /**
     * Send data to target and receive an answer
     *
     * @param dataToSend data for sending
     * @return response from target
     */
    public Identifiable sendAndReceiveData(final Identifiable dataToSend) {
        receivedData = null;

        final Object id = dataToSend.getId();
        listener.setIdListener(id, this, true);

        log.log(Level.CONFIG, "Starting conversation with id {0} to {1} - [{2}]", new Object[]{id.toString(), target.getAddress().getHostAddress(), dataToSend.toString()});
        target.sendData(dataToSend);

        synchronized (this) {
            if (receivedData == null) {
                try {
                    this.wait();
                } catch (InterruptedException ex) {
                    log.log(Level.WARNING, "Waiting of BasicConversator has been interrupted, result may not be actual data.", ex);
                }
            }
        }        

        listener.removeIdListener(id);
        
        return receivedData;
    }

    @Override
    public Object receiveData(Identifiable data) {
        receivedData = data;
        synchronized (this) {
            this.notify();
            log.log(Level.CONFIG, "Received reply for conversation with id {0} - [{1}]", new Object[]{data.getId(), data.toString()});
        }
        return true;
    }
}
