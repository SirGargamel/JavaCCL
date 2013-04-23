package cz.tul.comm.socket;

import java.util.UUID;

/**
 * Filter for communication filtering.
 *
 * @author Petr Ječmen
 */
public interface IDFilter {

    /**
     * Check, if given ID is allowed.
     *
     * @param id ID for filtering
     * @return true if ID is allowed
     */
    boolean isIdAllowed(final UUID id);
}
