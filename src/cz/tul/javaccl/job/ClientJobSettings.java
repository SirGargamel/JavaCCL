package cz.tul.javaccl.job;

import java.io.Serializable;
import java.util.UUID;

/**
 * Class containing new value of client settings.
 *
 * @author Petr Jecmen
 */
public class ClientJobSettings implements Serializable {

    private final UUID clientId;
    private final String settings;
    private final int value;

    /**
     * New intance
     *
     * @param clientId UUID of the client
     * @param settings which settings to change
     * @param value maximal count of concurrent jobs
     */
    public ClientJobSettings(final UUID clientId, final String settings, final int value) {
        this.clientId = clientId;
        this.value = value;
        this.settings = settings;
    }

    /**
     * @return UUID of the client
     */
    public UUID getClientId() {
        return clientId;
    }

    /**     
     * @return which settings has changed
     */
    public String getSettings() {
        return settings;
    }

    /**
     * @return maximal count of concurrent jobs
     */
    public int getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Client ID ");
        sb.append(clientId);
        sb.append(", ");
        sb.append(settings);
        sb.append(" - ");
        sb.append(value);
        
        return sb.toString();
    }
}
