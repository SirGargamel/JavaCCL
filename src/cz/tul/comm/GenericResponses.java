package cz.tul.comm;

import java.io.Serializable;

/**
 *
 * @author Petr Ječmen
 */
public enum GenericResponses implements Serializable {

    ILLEGAL_DATA,
    ILLEGAL_TARGET_ID,
    NOT_HANDLED_DIRECTLY,
    ERROR,    
    UNKNOWN_DATA,
    UUID_NOT_ALLOWED,
    UUID_UNKNOWN,
    PULL_NOT_NEEDED,
    OK,
    REPLY_LATER;
    
    
}
