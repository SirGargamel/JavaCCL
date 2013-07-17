package cz.tul.comm.job;

import java.io.Serializable;
import java.util.UUID;

/**
 *
 * @author Petr Jecmen
 */
public class JobCount implements Serializable {

    private final UUID clientId;
    private final int jobCount;

    public JobCount(UUID clientId, int jobCount) {
        this.clientId = clientId;
        this.jobCount = jobCount;
    }

    public UUID getClientId() {
        return clientId;
    }

    public int getJobCount() {
        return jobCount;
    }
    
}
