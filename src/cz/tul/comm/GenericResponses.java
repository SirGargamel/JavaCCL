package cz.tul.comm;

import java.io.Serializable;

/**
 * Generic responses for error / success signalization.
 *
 * @author Petr Ječmen
 */
public enum GenericResponses implements Serializable {

    /**
     * Receiving class could not parse received data.
     */
    ILLEGAL_DATA,
    /**
     * Illegal header found in message.
     */
    ILLEGAL_HEADER,
    /**
     * Data were delivered to client, whos ID was different than targetID.
     */
    ILLEGAL_TARGET_ID,
    /**
     * No registered listener found, data were given to general message
     * listeners.
     */
    NOT_HANDLED_DIRECTLY,
    /**
     * Source UUID is not permitted to communicate with target.
     */
    UUID_NOT_ALLOWED,
    /**
     * Received data with unknown identificator.
     */
    UUID_UNKNOWN,
    /**
     * Everything is alright.
     */
    OK,
    /**
     * Error occured during communication
     */
    CONNECTION_ERROR;
}
