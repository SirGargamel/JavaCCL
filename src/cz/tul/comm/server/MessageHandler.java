package cz.tul.comm.server;

import cz.tul.comm.socket.IResponseHandler;
import cz.tul.comm.socket.IMessageHandler;
import cz.tul.comm.Message;
import java.net.InetAddress;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Middle man between server socket and registered handlers. Stores received
 * data in queues, from which handlers can obtain data. Handler is notified each
 * time data object is received.
 *
 * @author Petr Ječmen
 */
public class MessageHandler extends Thread implements IMessageHandler, IResponseHandler {

    private static final Logger log = Logger.getLogger(MessageHandler.class.getName());
    private final Map<InetAddress, Object> handlersAddress;
    private final Map<UUID, Object> handlersId;
    private final Map<Object, Queue<Object>> messages;

    public MessageHandler() {
        handlersAddress = new ConcurrentHashMap<>();
        handlersId = new ConcurrentHashMap<>();
        messages = new ConcurrentHashMap<>();
    }

    @Override
    public void handleMessage(final InetAddress address, final Object msg) {
        if (msg != null) {
            if (msg instanceof Message) {
                final Message m = (Message) msg;

                // TODO check for system message
            }
            if (handlersAddress.containsKey(address)) {
                final Object owner = handlersAddress.get(address);
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
        if (handlersAddress.containsKey(address)) {
            log.log(Level.WARNING, "Handler for IP {0} already assigned, no changes made.", address.getHostAddress());
        } else {
            handlersAddress.put(address, owner);
            if (!messages.containsKey(owner)) {
                messages.put(owner, new ConcurrentLinkedQueue<>());
            }
        }
    }

    @Override
    public void registerResponse(final UUID id, final Object owner) {
        if (handlersId.containsKey(id)) {
            log.log(Level.WARNING, "Handler for UUID {0} already assigned, no changes made.", id.toString());
        } else {
            handlersId.put(id, owner);
            if (!messages.containsKey(owner)) {
                messages.put(owner, new ConcurrentLinkedQueue<>());
            }
        }
    }

    @Override
    public Queue<Object> getResponseQueue(final Object owner) {
        final Queue<Object> m = messages.get(owner);
        return m;
    }

    @Override
    public void deregisterResponse(final InetAddress address, final Object owner) {
        Object own = handlersAddress.get(address);
        if (own == owner) {
            handlersAddress.remove(address);
            if (!handlersId.containsValue(own)) {
                messages.remove(own);
            }
        } else {
            log.log(Level.WARNING, "Wrong owner for IP {0}, no changes made", address.getHostAddress());
        }
    }

    @Override
    public void deregisterResponse(final UUID id, final Object owner) {
        Object own = handlersId.get(id);
        if (own == owner) {
            handlersId.remove(id);
            if (!handlersAddress.containsValue(own)) {
                messages.remove(own);
            }
        } else {
            log.log(Level.WARNING, "Wrong owner for UUID {0}, no changes made", id.toString());
        }
    }
}
