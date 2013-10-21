package cz.tul.javaccl.job;

import java.io.Serializable;
import java.util.UUID;

/**
 *
 * @author Petr Jecmen
 */
public class ClientJobSettings implements Serializable {

    private final UUID clientId;
    private final int value;

    /**
     * New intance
     *
     * @param clientId UUID of the client
     * @param value maximal count of concurrent jobs
     */
    public ClientJobSettings(UUID clientId, int value) {
        this.clientId = clientId;
        this.value = value;
    }

    /**
     * @return UUID of the client
     */
    public UUID getClientId() {
        return clientId;
    }

    /**
     * @return maximal count of concurrent jobs
     */
    public int getValue() {
        return value;
    }
}
