package cz.tul.comm.socket;

import java.util.UUID;

/**
 *
 * @author Petr Ječmen
 */
public interface IDFilter {

    boolean isIdAllowed(final UUID id);
    
}
