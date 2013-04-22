package cz.tul.comm.socket.queue;

/**
 * Interface fot data identificatoin.
 *
 * @author Gargamel
 */
public interface Identifiable {

    /**
     * @return ID characterizing data or communication
     */
    Object getId();
}