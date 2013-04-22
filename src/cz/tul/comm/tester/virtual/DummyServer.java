package cz.tul.comm.tester.virtual;

import cz.tul.comm.job.Job;
import cz.tul.comm.server.Comm_Server;
import cz.tul.comm.server.Server;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author Petr Ječmen
 */
public class DummyServer {

    private final Server s;
    private final ExecutorService exec;    

    public DummyServer() {
        s = Comm_Server.initNewServer();
        exec = Executors.newCachedThreadPool();
    }

    public void submitJob(final Action action, final int repetitionCount) {
        final Work w = new Work(action, repetitionCount);
        final Job j = s.submitJob(w);
        exec.submit(new Waiter(j));
    }

    public void waitForJobs() {
        s.getJobManager().waitForAllJobs();                
    }

    private static class Waiter implements Runnable {

        private final Job job;

        Waiter(Job job) {
            this.job = job;
        }

        @Override
        public void run() {
            final Object result = job.getResult(true);
        }
    }
}
