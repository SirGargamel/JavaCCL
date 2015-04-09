package cz.tul.javaccl;

/**
 * Generic responses for error / success signalization.
 *
 * @author Petr Ječmen
 */
public enum GenericResponses {

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
     * No registered listeners / observers found, so message was not handled in
     * any way.
     */
    NOT_HANDLED,
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
     * Message could not be handled (the request is correct, but target could
     * not carry out the command)
     */
    CANCEL,
    /**
     * Error occured during communication
     */
    CONNECTION_ERROR,
    /**
     * Unspecified type of error
     */
    GENERAL_ERROR;
}
