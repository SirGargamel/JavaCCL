package cz.tul.comm.server;

import cz.tul.comm.socket.IResponseHandler;
import cz.tul.comm.socket.IMessageHandler;
import cz.tul.comm.Message;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Petr Ječmen
 */
public class MessageHandler implements IMessageHandler, IResponseHandler {

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
            // TOGO logging
            System.err.println("NULL message");
        }
    }

    @Override
    public void registerResponse(final InetAddress address, final Object owner) {
        handlers.put(address, owner);
    }

    @Override
    public Object pickupResponse(final Object owner) {
        Object m = messages.get(owner);
        return m;
    }
}
