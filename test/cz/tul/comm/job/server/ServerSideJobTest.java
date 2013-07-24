package cz.tul.comm.job.server;

import cz.tul.comm.job.JobStatus;
import java.util.Timer;
import java.util.TimerTask;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Petr Jecmen
 */
public class ServerSideJobTest {

    @Test
    public void testGetTask() {
        System.out.println("getTask");

        String task = "task";
        ServerSideJob job = new ServerSideJob(task, null);
        assertEquals(task, job.getTask());
    }

    /**
     * Test of getResult method, of class ServerSideJob.
     */
    @Test
    public void testGetResult() {
        System.out.println("getResult");

        final ServerSideJob job = new ServerSideJob(null, null);
        assertNull(job.getResult(false));
                
        final String expectedResult = "result";

        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                job.setResult(expectedResult);
            }
        };
        Timer t = new Timer();
        t.schedule(tt, 200);
        assertEquals(expectedResult, job.getResult(true));
    }

    /**
     * Test of isDone method, of class ServerSideJob.
     */
    @Test
    public void testIsDone() {
        System.out.println("isDone");
        System.out.println("isCanceled");
        ServerSideJob job = new ServerSideJob(null, null);

        job.setStatus(JobStatus.SUBMITTED);
        assertFalse(job.isDone());

        job.setStatus(JobStatus.SENT);
        assertFalse(job.isDone());

        job.setStatus(JobStatus.ACCEPTED);
        assertFalse(job.isDone());

        job.setStatus(JobStatus.FINISHED);
        assertTrue(job.isDone());

        job.setStatus(JobStatus.ERROR);
        assertTrue(job.isDone());

        job.setStatus(JobStatus.CANCELED);
        assertTrue(job.isDone());
    }

    @Test
    public void testIsCanceled() {
        System.out.println("isCanceled");
        ServerSideJob job = new ServerSideJob(null, null);

        job.setStatus(JobStatus.SUBMITTED);
        assertFalse(job.isCanceled());

        job.setStatus(JobStatus.SENT);
        assertFalse(job.isCanceled());

        job.setStatus(JobStatus.ACCEPTED);
        assertFalse(job.isCanceled());

        job.setStatus(JobStatus.FINISHED);
        assertFalse(job.isCanceled());

        job.setStatus(JobStatus.ERROR);
        assertTrue(job.isCanceled());

        job.setStatus(JobStatus.CANCELED);
        assertTrue(job.isCanceled());
    }
}