package cz.tul.comm.server;

import cz.tul.comm.socket.IResponseHandler;
import cz.tul.comm.socket.IMessageHandler;
import cz.tul.comm.Message;
import java.net.InetAddress;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class MessageHandler extends Thread implements IMessageHandler, IResponseHandler {

    private static final Logger log = Logger.getLogger(MessageHandler.class.getName());
    private final Map<InetAddress, Object> handlers;
    private final Map<Object, Queue<Object>> messages;

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
                messages.get(owner).add(msg);

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
        if (handlers.containsKey(address)) {
            log.log(Level.WARNING, "Handler fot IP {0} already assigned, no changes made.", address.getHostAddress());
        } else {
            handlers.put(address, owner);
            messages.put(owner, new ConcurrentLinkedQueue<>());
        }
    }

    @Override
    public Queue<Object> getResponseQueue(final Object owner) {
        final Queue<Object> m = messages.get(owner);
        return m;
    }

    @Override
    public void deregisterResponse(final InetAddress address, final Object owner) {
        Object own = handlers.get(address);
        if (own == owner) {
            handlers.remove(address);
            messages.remove(own);
        } else {
            log.log(Level.WARNING, "Wrong owner for IP {0}, no changes made", address.getHostAddress());
        }
    }
}
