package cz.tul.comm.server;

import cz.tul.comm.socket.IResponseHandler;
import cz.tul.comm.socket.IMessageHandler;
import cz.tul.comm.Message;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class MessageHandler implements IMessageHandler, IResponseHandler {

    private static final Logger log = Logger.getLogger(MessageHandler.class.getName());
    private final Map<InetAddress, Object> handlers;
    private final Map<Object, Object> messages;

    public MessageHandler() {
        handlers = new ConcurrentHashMap<>();
        messages = new ConcurrentHashMap<>();
    }

    @Override
    public void handleMessage(final InetAddress address, final Object msg) {
        if (msg != null) {
            if (msg instanceof Message) {
                Message m = (Message) msg;

                // TODO check for system message
            }
            if (handlers.containsKey(address)) {
                final Object owner = handlers.get(address);
                messages.put(owner, msg);

                synchronized (owner) {
                    owner.notify();
                }
            }

        } else {
            log.warning("NULL message received.");
        }
    }

    @Override
    public void registerResponse(final InetAddress address, final Object owner) {
        handlers.put(address, owner);
    }

    @Override
    public Object pickupResponse(final Object owner) {
        final Object m = messages.get(owner);
        return m;
    }
}
