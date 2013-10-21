package cz.tul.javaccl.socket;

import java.util.UUID;

/**
 * Filter for communication filtering.
 *
 * @author Petr Ječmen
 */
public interface IDFilter {

    /**
     * @return ID of local instance
     */
    UUID getLocalID();

    /**
     * Check if the given UUID is allowed to communicate with this instance
     *
     * @param id UUID for checking
     * @return true if the communication is allowed
     */
    boolean isTargetIdValid(final UUID id);

    /**
     * Check, if given ID is allowed.
     *
     * @param id ID for filtering
     * @return true if ID is allowed
     */
    boolean isIdAllowed(final UUID id);
}
