package cz.tul.comm;

import cz.tul.comm.client.Client;
import cz.tul.comm.client.ClientImpl;
import cz.tul.comm.communicator.CommunicatorImpl;
import cz.tul.comm.exceptions.ConnectionException;
import cz.tul.comm.job.client.Assignment;
import cz.tul.comm.job.client.AssignmentListener;
import cz.tul.comm.job.server.Job;
import cz.tul.comm.server.DataStorage;
import cz.tul.comm.server.Server;
import cz.tul.comm.server.ServerImpl;
import cz.tul.comm.socket.ServerSocket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;

/**
 *
 * @author Petr Jecmen
 */
public class JobTest {

    private Server s;
    private Client c;

//    @BeforeClass
//    public static void setUpClass() {
//        Utils.adjustMainHandlersLoggingLevel(Level.CONFIG);
//        Utils.adjustMainLoggerLevel(Level.CONFIG);
//        Utils.adjustClassLoggingLevel(ServerSocket.class, Level.INFO);
//        Utils.adjustClassLoggingLevel(ClientImpl.class, Level.SEVERE);
//        Utils.adjustClassLoggingLevel(CommunicatorImpl.class, Level.WARNING);
//    }
//
//    @AfterClass
//    public static void tearDownClass() {
//        Utils.adjustMainHandlersLoggingLevel(Level.INFO);
//        Utils.adjustMainLoggerLevel(Level.INFO);
//    }

    @Before
    public void setUp() {
        try {
            s = ServerImpl.initNewServer();
            c = ClientImpl.initNewClient();
            c.registerToServer(InetAddress.getLoopbackAddress());
        } catch (ConnectionException ex) {
            fail("Initialization failed - " + ex);
        }
    }

    @After
    public void tearDown() {
        c.stopService();
        s.stopService();
    }

    @Test
    public void testBasicJob() {
        final Counter counter = new Counter();

        c.setAssignmentListener(new AssignmentListener() {
            @Override
            public void receiveTask(Assignment task) {
                try {
                    Object tsk = task.getTask();

                    if (!(tsk instanceof Integer)) {
                        fail("Illegal task received");
                    }

                    Integer count = (Integer) tsk;

                    synchronized (JobTest.this) {
                        JobTest.this.wait(count);
                    }

                    counter.add(count);

                    task.submitResult(GenericResponses.OK);
                } catch (ConnectionException ex) {
                    fail("Connection to servr failed - " + ex);
                } catch (InterruptedException ex) {
                    fail("Waiting has been interrupted - " + ex);
                }
            }

            @Override
            public void cancelTask(Assignment task) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });

        final int cnt = 5000;
        Job j = s.submitJob(cnt);
        s.getJobManager().waitForAllJobs();

        assertEquals(GenericResponses.OK, j.getResult(true));
        assertEquals(cnt, counter.getCount());
    }

    @Test
    public void testBasicJobWithDataRequest() {
        final Counter counter = new Counter();

        final String dataTitle = "dataId";
        final int taskCount = 5000;
        final int requestCount = 3000;

        c.setAssignmentListener(new AssignmentListener() {
            @Override
            public void receiveTask(Assignment task) {
                try {
                    Object tsk = task.getTask();

                    if (!(tsk instanceof Integer)) {
                        fail("Illegal task received");
                    }

                    Integer count = (Integer) tsk;

                    synchronized (JobTest.this) {
                        JobTest.this.wait(count);
                    }

                    counter.add(count);

                    Object req = task.requestData(dataTitle);
                    if (req instanceof Integer) {
                        counter.add((int) req);
                    } else {
                        fail("Illegal data from server data request - " + req);
                    }

                    task.submitResult(GenericResponses.OK);
                } catch (ConnectionException ex) {
                    fail("Connection to servr failed - " + ex);
                } catch (InterruptedException ex) {
                    fail("Waiting has been interrupted - " + ex);
                }
            }

            @Override
            public void cancelTask(Assignment task) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });

        s.assignDataStorage(new DataStorage() {
            @Override
            public Object requestData(Object dataId) {
                if (dataId.equals(dataTitle)) {
                    return requestCount;
                } else {
                    return null;
                }
            }
        });

        Job j = s.submitJob(taskCount);
        s.getJobManager().waitForAllJobs();

        assertEquals(GenericResponses.OK, j.getResult(true));
        assertEquals(taskCount + requestCount, counter.getCount());
    }

    @Test
    public void testMultipleJobs() {
        final Counter counter = new Counter();

        c.setAssignmentListener(new AssignmentListener() {
            @Override
            public void receiveTask(Assignment task) {
                try {
                    Object tsk = task.getTask();

                    if (!(tsk instanceof Integer)) {
                        fail("Illegal task received");
                    }

                    Integer count = (Integer) tsk;

                    synchronized (JobTest.this) {
                        System.out.println("Waiting " + count + "ms.");
                        JobTest.this.wait(count);
                    }

                    counter.add(count);

                    task.submitResult(GenericResponses.OK);
                } catch (ConnectionException ex) {
                    fail("Connection to servr failed - " + ex);
                } catch (InterruptedException ex) {
                    fail("Waiting has been interrupted - " + ex);
                }
            }

            @Override
            public void cancelTask(Assignment task) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });

        int cnt = 0, val;
        Random rnd = new Random();
        final int jobCount = rnd.nextInt(5) + 5;
        Set<Job> jobs = new HashSet<>(jobCount);
        for (int i = 0; i < jobCount; i++) {
            val = (rnd.nextInt(4) + 1) * 10;
            cnt += val;
            jobs.add(s.submitJob(val));
        }

        s.getJobManager().waitForAllJobs();

        for (Job j : jobs) {
            assertEquals(GenericResponses.OK, j.getResult(true));
        }

        assertEquals(cnt, counter.getCount());
    }

    @Test
    public void testMultipleJobsDataRequests() {
        final Counter totalCounter = new Counter();
        final Counter requestCounter = new Counter();

        final String dataTitle = "dataId";
        final double requestMultiplier = 2;

        c.setAssignmentListener(new AssignmentListener() {
            @Override
            public void receiveTask(Assignment task) {
                try {
                    Object tsk = task.getTask();

                    if (!(tsk instanceof Integer)) {
                        fail("Illegal task received");
                    }

                    Object req = task.requestData(dataTitle);
                    if (req instanceof Double) {
                        double multiplier = (double) req;

                        int count = (int) (((int) tsk) * multiplier);

                        synchronized (JobTest.this) {
                            JobTest.this.wait(count);
                        }

                        totalCounter.add(count);
                    } else {
                        fail("Illegal data from server data request - " + req);
                    }

                    task.submitResult(GenericResponses.OK);
                } catch (ConnectionException ex) {
                    fail("Connection to servr failed - " + ex);
                } catch (InterruptedException ex) {
                    fail("Waiting has been interrupted - " + ex);
                }
            }

            @Override
            public void cancelTask(Assignment task) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });

        s.assignDataStorage(new DataStorage() {
            @Override
            public Object requestData(Object dataId) {
                if (dataId.equals(dataTitle)) {
                    requestCounter.add(1);
                    return requestMultiplier;
                } else {
                    return null;
                }
            }
        });

        int cnt = 0, val;
        Random rnd = new Random();
        final int jobCount = rnd.nextInt(5) + 5;
        Set<Job> jobs = new HashSet<>(jobCount);
        for (int i = 0; i < jobCount; i++) {
            val = (rnd.nextInt(4) + 1) * 10;
            cnt += val;
            jobs.add(s.submitJob(val));
        }

        s.getJobManager().waitForAllJobs();

        for (Job j : jobs) {
            assertEquals(GenericResponses.OK, j.getResult(true));
        }

        assertEquals(cnt * requestMultiplier, totalCounter.getCount(), 0);
        
        assertEquals(jobCount, requestCounter.getCount());
    }

    @Test
    public void testMultipleJobsWithMultipleClients() {
        c.stopService();
        
        final Random rnd = new Random();

        final Counter counter = new Counter();
        final Set<AssignmentListener> computingClients = new HashSet<>();

        final int clientCount = rnd.nextInt(5) + 2;
        Client cl;
        for (int i = 0; i < clientCount; i++) {
            try {
                cl = ClientImpl.initNewClient();
                cl.registerToServer(InetAddress.getLoopbackAddress());
                cl.setAssignmentListener(new AssignmentListener() {
            @Override
            public void receiveTask(Assignment task) {
                try {
                    Object tsk = task.getTask();

                    if (!(tsk instanceof Integer)) {
                        fail("Illegal task received");
                    }

                    Integer count = (Integer) tsk;

                    synchronized (JobTest.this) {
                        JobTest.this.wait(count);
                    }

                    counter.add(count);

                    computingClients.add(this);

                    task.submitResult(GenericResponses.OK);
                } catch (ConnectionException ex) {
                    fail("Connection to servr failed - " + ex);
                } catch (InterruptedException ex) {
                    fail("Waiting has been interrupted - " + ex);
                }
            }

            @Override
            public void cancelTask(Assignment task) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });
            } catch (ConnectionException ex) {
                fail("Initialization of one of the clients failed - " + ex);
            }
        }        

        int cnt = 0, val;
        final int jobCount = clientCount * (rnd.nextInt(3) + 2);
        for (int i = 0; i < jobCount; i++) {
            val = (rnd.nextInt(4) + 1) * 10;
            cnt += val;
            s.submitJob(val);
        }        

        s.getJobManager().waitForAllJobs();

        // check all result if OK
        for (Job j : s.getJobManager().getAllJobs()) {
            assertEquals(GenericResponses.OK, j.getResult(true));
        }

        assertEquals(cnt, counter.getCount());
        assertEquals(clientCount, computingClients.size());
    }    

    @Test
    public void testMultipleJobsWithMultipleClientsWithDataRequests() {
        c.stopService();               
        
        final Random rnd = new Random();

        final Counter totalCounter = new Counter();
        final Counter requestCounter = new Counter();
        final Set<AssignmentListener> computingClients = new HashSet<>();
        
        final String dataTitle = "dataId";
        final double requestMultiplier = 2;
        
        s.assignDataStorage(new DataStorage() {
            @Override
            public Object requestData(Object dataId) {
                if (dataId.equals(dataTitle)) {
                    requestCounter.add(1);
                    return requestMultiplier;
                } else {
                    return null;
                }
            }
        });

        final int clientCount = rnd.nextInt(5) + 2;
        Client cl;
        for (int i = 0; i < clientCount; i++) {
            try {
                cl = ClientImpl.initNewClient();
                cl.registerToServer(InetAddress.getLoopbackAddress());
                cl.setAssignmentListener(new AssignmentListener() {
            @Override
            public void receiveTask(Assignment task) {
                try {
                    Object tsk = task.getTask();

                    if (!(tsk instanceof Integer)) {
                        fail("Illegal task received");
                    }

                    Object req = task.requestData(dataTitle);
                    if (req instanceof Double) {
                        double multiplier = (double) req;

                        int count = (int) (((int) tsk) * multiplier);

                        synchronized (JobTest.this) {
                            JobTest.this.wait(count);
                        }

                        totalCounter.add(count);
                    } else {
                        fail("Illegal data from server data request - " + req);
                    }

                    computingClients.add(this);

                    task.submitResult(GenericResponses.OK);
                } catch (ConnectionException ex) {
                    fail("Connection to servr failed - " + ex);
                } catch (InterruptedException ex) {
                    fail("Waiting has been interrupted - " + ex);
                }
            }

            @Override
            public void cancelTask(Assignment task) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });
            } catch (ConnectionException ex) {
                fail("Initialization of one of the clients failed - " + ex);
            }
        }        

        int cnt = 0, val;
        final int jobCount = clientCount * (rnd.nextInt(3) + 2);
        for (int i = 0; i < jobCount; i++) {
            val = (rnd.nextInt(4) + 1) * 10;
            cnt += val;
            s.submitJob(val);
        }        

        s.getJobManager().waitForAllJobs();

        // check all result if OK
        for (Job j : s.getJobManager().getAllJobs()) {
            assertEquals(GenericResponses.OK, j.getResult(true));
        }

        assertEquals(cnt * requestMultiplier, totalCounter.getCount(), 0);
        
        assertEquals(clientCount, computingClients.size());
        
        assertEquals(jobCount, requestCounter.getCount());
    }

    private class Counter {

        int count;

        public Counter() {
            count = 0;
        }

        public synchronized void add(final int count) {
            this.count += count;
        }

        public synchronized int getCount() {
            return this.count;
        }
    }
}