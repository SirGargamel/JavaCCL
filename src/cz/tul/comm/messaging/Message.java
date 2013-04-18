package cz.tul.comm.messaging;

import cz.tul.comm.socket.queue.IIdentifiable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Basic communication object between server and client.
 *
 * @author Petr Ječmen
 */
public class Message implements Serializable, IIdentifiable {

    private static final long serialVersionUID = 1L;
    private final UUID id;
    private final String header;
    private final Object data;

    /**
     *
     * @param id UUID of message
     * @param header message description
     * @param data message data
     */
    public Message(final UUID id, final String header, final Object data) {
        if (data != null && !(data instanceof Serializable)) {
            throw new IllegalArgumentException("Data object needs to implement Serializable interface in order to be able to send it.");
        }

        this.id = id;
        this.header = header;
        this.data = data;
    }

    /**
     *
     * @param header message description
     * @param data message data
     */
    public Message(final String header, final Object data) {
        this(UUID.randomUUID(), header, data);
    }

    @Override
    public UUID getId() {
        return id;
    }

    /**
     *
     * @return message description
     */
    public String getHeader() {
        return header;
    }

    /**
     *
     * @return message data
     */
    public Object getData() {
        return data;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(); 
        result.append(id);
        result.append(" - ");
        result.append(header);
        result.append(" - ");
        if (data != null) {
            result.append(data.toString());
        }        

        return result.toString();
    }

    @Override
    public boolean equals(Object o) {
        boolean result = false;

        if (o instanceof Message) {
            final Message m = (Message) o;
            result = m.hashCode() == hashCode() ? true : false;
        }

        return result;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + Objects.hashCode(this.id);
        hash = 29 * hash + Objects.hashCode(this.header);
        hash = 29 * hash + Objects.hashCode(this.data);
        return hash;
    }
}
