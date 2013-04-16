package cz.tul.comm.tester.virtual;

import cz.tul.comm.client.Client;
import cz.tul.comm.client.Comm_Client;
import cz.tul.comm.messaging.job.Assignment;
import cz.tul.comm.messaging.job.IAssignmentListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * dummy client for testing.
 *
 * @author Petr Ječmen
 */
public class DummyClient implements IAssignmentListener, IClose {

    private static final Logger log = Logger.getLogger(DummyClient.class.getName());
    private final Client c;
    private Assignment currentTask;
    private Future currentFuture;
    private final ExecutorService exec;

    /**
     *
     */
    public DummyClient() {
        c = Comm_Client.initNewClient();
        exec = Executors.newCachedThreadPool();
    }

    @Override
    public void receiveTask(Assignment task) {
        if (currentTask != null) {
            log.config("Client is already computing.");
        } else {
            final Object run = task.getTask();
            if (run instanceof Runnable) {
                final Runnable r = (Runnable) run;
                currentFuture = exec.submit(r);

                try {
                    final Object result = currentFuture.get();
                    task.submitResult(result);
                } catch (InterruptedException | ExecutionException ex) {
                    log.log(Level.WARNING, "Error computing result.", ex.getLocalizedMessage());
                }
            }
        }
    }

    @Override
    public void cancelTask(Assignment task) {
        if (currentTask != null) {
            log.config("Cancelling current task.");
            currentFuture.cancel(true);
        } else {
            log.config("No task to cancel.");
        }
    }

    @Override
    public void closeClient() {
        c.stopService();
    }
}
