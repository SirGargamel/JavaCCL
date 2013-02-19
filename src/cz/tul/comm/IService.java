package cz.tul.comm;

/**
 * Interface for running services, that should be stopped in orderly
 * fashion before shutdown.
 * @author Petr Ječmen
 */
public interface IService {

    void stopService();

}
