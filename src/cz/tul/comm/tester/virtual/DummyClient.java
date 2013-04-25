package cz.tul.comm.tester.virtual;

import cz.tul.comm.Constants;
import cz.tul.comm.client.Client;
import cz.tul.comm.client.ClientImpl;
import cz.tul.comm.job.Assignment;
import cz.tul.comm.job.AssignmentListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
public class DummyClient implements AssignmentListener {

    private static final Logger log = Logger.getLogger(DummyClient.class.getName());
    private final Client c;
    private Assignment currentTask;
    private Future currentFuture;
    private final ExecutorService exec;
    private double errorChance, fatalChance;

    /**
     *
     */
    private DummyClient(final double errorChance, final double fatalChance) {
        c = ClientImpl.initNewClient();
        exec = Executors.newCachedThreadPool();
        this.errorChance = errorChance;
        this.fatalChance = fatalChance;
    }

    private void init() {
        c.assignAssignmentListener(this);
    }

    public void connectToServer(final String address) {
        try {
            log.log(Level.INFO, "Loggin to server at IP {0}", address);
            c.registerToServer(InetAddress.getByName(address), Constants.DEFAULT_PORT);
        } catch (UnknownHostException ex) {
            Logger.getLogger(DummyClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void receiveTask(Assignment task) {
        if (currentTask != null && currentTask.isDone()) {
            currentTask = null;
            currentFuture = null;
        }

        if (currentTask != null) {
            log.config("Client is already computing.");
            task.cancel("Already computing.");
        } else {
            log.config("Received work, starting computation.");

            currentTask = task;
            final Object work = currentTask.getTask();
            if (work instanceof Work) {
                final Work w = (Work) work;
                currentFuture = exec.submit(w);

                final double chance = Math.random();
                if (chance > (1 - fatalChance)) {
                    synchronized (this) {
                        try {
                            this.wait((long) (Math.random() * w.getRepetitionCount()));
                        } catch (InterruptedException ex) {
                            log.warning("Waiting for client close has been interrupted.");
                        }
                    }
                    c.stopService();
                } else if (chance > (1 - (fatalChance + errorChance))) {
                    synchronized (this) {
                        try {
                            this.wait((long) (Math.random() * w.getRepetitionCount()));
                        } catch (InterruptedException ex) {
                            log.warning("Waiting for client cancelation has been interrupted.");
                        }
                    }
                    task.cancel("Client error.");
                } else {
                    try {
                        task.submitResult(currentFuture.get());
                    } catch (InterruptedException | ExecutionException ex) {
                        log.log(Level.WARNING, "Error computing result.\n{0}", ex.getLocalizedMessage());
                    }
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

    /**
     *
     * @param errorChance
     * @param fatalChance
     * @return
     */
    public static DummyClient newInstance(final double errorChance, final double fatalChance) {
        DummyClient result = new DummyClient(errorChance, fatalChance);
        result.init();
        log.log(Level.CONFIG, "New DummyClient created with chances {0}, {1}.", new Object[]{errorChance, fatalChance});
        return result;
    }
}
