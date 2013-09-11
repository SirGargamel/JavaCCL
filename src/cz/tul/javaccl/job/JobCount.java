package cz.tul.javaccl.job;

import java.io.Serializable;
import java.util.UUID;

/**
 *
 * @author Petr Jecmen
 */
public class JobCount implements Serializable {

    private final UUID clientId;
    private final int jobCount;

    /**
     * New intance
     *
     * @param clientId UUID of the client
     * @param jobCount maximal count of concurrent jobs
     */
    public JobCount(UUID clientId, int jobCount) {
        this.clientId = clientId;
        this.jobCount = jobCount;
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
    public int getJobCount() {
        return jobCount;
    }
}
