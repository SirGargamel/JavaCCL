package cz.tul.comm.socket;

import cz.tul.comm.messaging.Message;
import cz.tul.comm.socket.queue.IListener;
import java.net.InetAddress;
import java.util.Queue;
import java.util.UUID;

/**
 *
 * @author Petr Ječmen
 */
public interface IListenerRegistrator {

    Queue<Object> addDataListener(final InetAddress address, final IListener<InetAddress, Object> dataListener);

    void removeDataListener(final InetAddress address, final IListener<InetAddress, Object> dataListener);

    void removeDataListener(final IListener<InetAddress, Object> dataListener);

    Queue<Message> addUUIDListener(final UUID id, final IListener<UUID, Message> idListener);

    void removeUUIDListener(final UUID id, final IListener<UUID, Message> idListener);

    void removeUUIDListener(final IListener<UUID, Message> idListener);
}
