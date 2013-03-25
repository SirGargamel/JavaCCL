package cz.tul.comm;

/**
 * Interface for running services, which should be stopped in orderly fashion
 * before shutdown.
 *
 * @author Petr Ječmen
 */
public interface IService {

    /**
     * Orderly stop of given service.
     */
    void stopService();
}
