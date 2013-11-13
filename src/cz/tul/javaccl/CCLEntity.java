package cz.tul.javaccl;

import java.util.UUID;

/**
 *
 * @author Petr Jecmen
 */
public class CCLEntity extends CCLObservable {
    
    private final UUID id;
    
    public CCLEntity() {
        id = UUID.randomUUID();
    }
    
    public UUID getId() {
        return id;
    }
    
}
