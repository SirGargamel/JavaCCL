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

    public void submitJob(final int repetitionCount) {
        final Work w = new Work(repetitionCount);
        final Job j = s.submitJob(w);
        w.setJobId(j.getId());
        exec.submit(new Waiter(j, w));
    }

    public void waitForJobs() {
        s.getJobManager().waitForAllJobs();                
    }

    private static class Waiter implements Runnable {

        private final Job job;
        private final Work work;

        Waiter(Job job, Work work) {
            this.job = job;
            this.work = work;
        }

        @Override
        public void run() {
            final Object result = job.getResult(true);
            if (!result.equals(work.buildResult())) {
                System.out.println("Result mismatch - [" + result.toString() + "] vs [" + work.buildResult() + "]");
            }
        }
    }
}
