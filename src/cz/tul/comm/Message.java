package cz.tul.comm;

import java.io.Serializable;
import java.util.UUID;

/**
 * Basic communication object between server and client.
 * @author Petr Ječmen
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String header;
    private final Object data;

    public Message(UUID id, String header, Object data) {
        if (!(data instanceof Serializable)) {
            throw new IllegalArgumentException("Data object needs to implement Serializable interface in order to be able to send it.");
        }

        this.id = id;
        this.header = header;
        this.data = data;
    }

    public UUID getId() {
        return id;
    }

    public String getHeader() {
        return header;
    }

    public Object getData() {
        return data;
    }

    @Override
    public String toString() {
        return header.concat(" - ").concat(data.toString());
    }
}
