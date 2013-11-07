package cz.tul.javaccl.job;

import cz.tul.javaccl.GlobalConstants;
import cz.tul.javaccl.GenericResponses;
import cz.tul.javaccl.client.Client;
import cz.tul.javaccl.client.ClientImpl;
import cz.tul.javaccl.exceptions.ConnectionException;
import cz.tul.javaccl.job.client.Assignment;
import cz.tul.javaccl.job.client.AssignmentListener;
import cz.tul.javaccl.job.server.Job;
import cz.tul.javaccl.server.DataStorage;
import cz.tul.javaccl.server.Server;
import cz.tul.javaccl.server.ServerImpl;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Petr Jecmen
 */
public class JobTest {

    private Server s;
    private Client c;

    @Before
    public void setUp() {
        try {
            s = ServerImpl.initNewServer();
            c = ClientImpl.initNewClient(5253);
            c.registerToServer(InetAddress.getByName(GlobalConstants.IP_LOOPBACK));
        } catch (Exception ex) {
            fail("Initialization failed - " + ex);
        }
    }

    @After
    public void tearDown() {
        if (c != null) {
            c.stopService();
        }
        if (s != null) {
            s.stopService();
        }
    }

    @Test
    public void testBasicJob() {
        System.out.println("basicJob");
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
        System.out.println("basicJobWithDataRequest");
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
                        counter.add((Integer) req);
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
        System.out.println("multipleJobs");
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

        int cnt = 0, val;
        Random rnd = new Random();
        final int jobCount = rnd.nextInt(5) + 5;
        Set<Job> jobs = new HashSet<Job>(jobCount);
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
        System.out.println("multipleJobsWithDataRequests");
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
                        double multiplier = (Double) req;

                        int count = (int) (((Integer) tsk) * multiplier);

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
        Set<Job> jobs = new HashSet<Job>(jobCount);
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
        System.out.println("multipleJobsWithMultipleClients");
        c.stopService();

        final Random rnd = new Random();

        final Counter counter = new Counter();
        final Set<AssignmentListener> computingClients = new HashSet<AssignmentListener>();

        final int clientCount = rnd.nextInt(5) + 2;
        Client cl;
        List<Client> clients = new ArrayList<Client>(clientCount);
        for (int i = 0; i < clientCount; i++) {
            try {
                cl = ClientImpl.initNewClient();
                cl.registerToServer(InetAddress.getByName(GlobalConstants.IP_LOOPBACK));
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
                clients.add(cl);
            } catch (Exception ex) {
                fail("Initialization of one of the clients failed - " + ex);
            }
        }

        int cnt = 0, val;
        final int jobCount = clientCount * (rnd.nextInt(3) + 2);
        Set<Job> allJobs = new HashSet<Job>(jobCount);
        for (int i = 0; i < jobCount; i++) {
            val = (rnd.nextInt(4) + 1) * 10;
            cnt += val;
            allJobs.add(s.submitJob(val));
        }

        s.getJobManager().waitForAllJobs();

        // check all result if OK
        for (Job j : allJobs) {
            assertEquals(GenericResponses.OK, j.getResult(true));
        }

        assertEquals(cnt, counter.getCount());
        assertEquals(clientCount, computingClients.size());

        for (Client client : clients) {
            client.stopService();
        }
    }

    @Test
    public void testMultipleJobsWithMultipleClientsWithDataRequests() {
        System.out.println("multipleJobsWithMultipleClientsWithDataRequests");
        c.stopService();

        final Random rnd = new Random();

        final Counter totalCounter = new Counter();
        final Counter requestCounter = new Counter();
        final Set<AssignmentListener> computingClients = new HashSet<AssignmentListener>();

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
        List<Client> clients = new ArrayList<Client>(clientCount);
        for (int i = 0; i < clientCount; i++) {
            try {
                cl = ClientImpl.initNewClient();
                cl.registerToServer(InetAddress.getByName(GlobalConstants.IP_LOOPBACK));
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
                                double multiplier = (Double) req;

                                int count = (int) (((Integer) tsk) * multiplier);

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
                clients.add(cl);
            } catch (Exception ex) {
                fail("Initialization of one of the clients failed - " + ex);
            }
        }

        int cnt = 0, val;
        final int jobCount = clientCount * (rnd.nextInt(3) + 2);
        Set<Job> allJobs = new HashSet<Job>(jobCount);
        for (int i = 0; i < jobCount; i++) {
            val = (rnd.nextInt(4) + 1) * 10;
            cnt += val;
            allJobs.add(s.submitJob(val));
        }

        s.getJobManager().waitForAllJobs();

        // check all result if OK
        for (Job j : allJobs) {
            assertEquals(GenericResponses.OK, j.getResult(true));
        }

        assertEquals(cnt * requestMultiplier, totalCounter.getCount(), 0);

        assertEquals(clientCount, computingClients.size());

        assertEquals(jobCount, requestCounter.getCount());

        for (Client client : clients) {
            client.stopService();
        }
    }

    @Test
    public void testMultipleConcurrentJobsOnClient() {
        System.out.println("multipleConcurrentJobsOnClient");
        final Counter counter = new Counter();
        final Set<AssignmentListener> computingClients = new HashSet<AssignmentListener>();

        final int concurrentCount = 3;
        final Counter concurrentCounter = new Counter();
        c.setMaxNumberOfConcurrentAssignments(concurrentCount);
        c.setAssignmentListener(new AssignmentListener() {
            @Override
            public void receiveTask(Assignment task) {
                try {
                    concurrentCounter.add(1);
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
                    concurrentCounter.sub(1);
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

        final Random rnd = new Random();

        final int clientCount = rnd.nextInt(2) + 1;
        Client cl;
        List<Client> clients = new ArrayList<Client>(clientCount);
        for (int i = 0; i < clientCount; i++) {
            try {
                cl = ClientImpl.initNewClient();
                cl.registerToServer(InetAddress.getByName(GlobalConstants.IP_LOOPBACK));
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
                clients.add(cl);
            } catch (Exception ex) {
                fail("Initialization of one of the clients failed - " + ex);
            }
        }

        int cnt = 0, val;
        final int jobCount = clientCount * (rnd.nextInt(6) + 5);
        Set<Job> allJobs = new HashSet<Job>(jobCount);
        for (int i = 0; i < jobCount; i++) {
            val = (rnd.nextInt(4) + 1) * 10;
            cnt += val;
            allJobs.add(s.submitJob(val));
        }

        s.getJobManager().waitForAllJobs();

        // check all result if OK
        for (Job j : allJobs) {
            assertEquals(GenericResponses.OK, j.getResult(true));
        }

        assertEquals(cnt, counter.getCount());
        assertEquals(clientCount + 1, computingClients.size());
        assertEquals(concurrentCount, concurrentCounter.getMax());

        for (Client client : clients) {
            client.stopService();
        }
    }

    @Test
    public void testNotRespondingClient() {
        System.out.println("notResponsindClient");
        try {
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
                        fail("Connection to server failed - " + ex);
                    } catch (InterruptedException ex) {
                        fail("Waiting has been interrupted - " + ex);
                    }
                }

                @Override
                public void cancelTask(Assignment task) {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
            });

            final Client failingClient = ClientImpl.initNewClient(5254);

            try {
                failingClient.registerToServer(InetAddress.getByName(GlobalConstants.IP_LOOPBACK));
            } catch (ConnectionException ex) {
                fail("Failed to connect client to server - " + ex);
            }
            // not accepting client
            failingClient.setAssignmentListener(new AssignmentListener() {
                @Override
                public void receiveTask(Assignment task) {
                    // do nothing, no accept
                }

                @Override
                public void cancelTask(Assignment task) {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
            });

            int cnt = 0, val;
            Random rnd = new Random();
            final int jobCount = rnd.nextInt(5) + 5;
            Set<Job> jobs = new HashSet<Job>(jobCount);
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

            failingClient.stopService();
        } catch (IOException ex) {
            fail("Failed to initialize client.");
        }
    }

    @Test
    public void testCancelingClient() {
        System.out.println("cancelingClient");
        try {
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
                        fail("Connection to server failed - " + ex);
                    } catch (InterruptedException ex) {
                        fail("Waiting has been interrupted - " + ex);
                    }
                }

                @Override
                public void cancelTask(Assignment task) {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
            });

            final Client failingClient = ClientImpl.initNewClient(5254);
            try {
                failingClient.registerToServer(InetAddress.getByName(GlobalConstants.IP_LOOPBACK));
            } catch (ConnectionException ex) {
                fail("Failed to connect client to server - " + ex);
            }
            //canceling client
            failingClient.setAssignmentListener(new AssignmentListener() {
                @Override
                public void receiveTask(Assignment task) {
                    try {
                        Object w = task.getTask();

                        if (w instanceof Integer) {
                            synchronized (JobTest.this) {
                                JobTest.this.wait(((Integer) w) / 2);
                            }

                            task.cancel("Moody client.");
                        } else {
                            fail("Illegal task received - " + w);
                        }
                    } catch (ConnectionException ex) {
                        fail("Connection to server failed - " + ex);
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
            Set<Job> jobs = new HashSet<Job>(jobCount);
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

            failingClient.stopService();
        } catch (IOException ex) {
            fail("Failed to initialize client.");
        }
    }

    @Test
    public void testCancelingClients() {
        System.out.println("cancelingClients");
        try {
            final Counter counter = new Counter();

            c.setAssignmentListener(new AssignmentListener() {
                @Override
                public void receiveTask(Assignment task) {
                    try {
                        Object tsk = task.getTask();
                        if (!(tsk instanceof Integer)) {
                            fail("Illegal task received");
                        }

                        if (Math.random() < 0.25) {
                            synchronized (JobTest.this) {
                                JobTest.this.wait(((Integer) tsk) / 4);
                            }
                            task.cancel("Client cancel.");
                        } else {
                            Integer count = (Integer) tsk;
                            synchronized (JobTest.this) {
                                JobTest.this.wait(count);
                            }
                            counter.add(count);
                            task.submitResult(GenericResponses.OK);
                        }

                    } catch (ConnectionException ex) {
                        fail("Connection to server failed - " + ex);
                    } catch (InterruptedException ex) {
                        fail("Waiting has been interrupted - " + ex);
                    }
                }

                @Override
                public void cancelTask(Assignment task) {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
            });

            final Client failingClient = ClientImpl.initNewClient(5254);
            try {
                failingClient.registerToServer(InetAddress.getByName(GlobalConstants.IP_LOOPBACK));
            } catch (ConnectionException ex) {
                fail("Failed to connect client to server - " + ex);
            }
            //canceling client
            failingClient.setAssignmentListener(new AssignmentListener() {
                @Override
                public void receiveTask(Assignment task) {
                    try {
                        Object tsk = task.getTask();
                        if (!(tsk instanceof Integer)) {
                            fail("Illegal task received");
                        }

                        if (Math.random() < 0.25) {
                            synchronized (JobTest.this) {
                                JobTest.this.wait(((Integer) tsk) / 4);
                            }
                            task.cancel("Client cancel.");
                        } else {
                            Integer count = (Integer) tsk;
                            synchronized (JobTest.this) {
                                JobTest.this.wait(count);
                            }
                            counter.add(count);
                            task.submitResult(GenericResponses.OK);
                        }

                    } catch (ConnectionException ex) {
                        fail("Connection to server failed - " + ex);
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
            Set<Job> jobs = new HashSet<Job>(jobCount);
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

            failingClient.stopService();
        } catch (IOException ex) {
            fail("Failed to initialize client.");
        }
    }

    @Test
    public void testOfflineClient() {
        System.out.println("offlineClient");
        try {
            final Counter counter = new Counter();
            final int multiplier = 3;

            final AssignmentListener al = new AssignmentListener() {
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

                        task.submitResult(multiplier * count);
                    } catch (ConnectionException ex) {
                        fail("Connection to server failed - " + ex);
                    } catch (InterruptedException ex) {
                        fail("Waiting has been interrupted - " + ex);
                    }
                }

                @Override
                public void cancelTask(Assignment task) {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
            };

            c.setAssignmentListener(al);

            final Client failingClient = ClientImpl.initNewClient(5254);
            try {
                failingClient.registerToServer(InetAddress.getByName(GlobalConstants.IP_LOOPBACK));
            } catch (ConnectionException ex) {
                fail("Failed to connect client to server - " + ex);
            }

            failingClient.setAssignmentListener(al);

            int totalCount = 0, val;
            Random rnd = new Random();
            final int jobCount = rnd.nextInt(5) + 5;
            Set<Job> jobs = new HashSet<Job>(jobCount);
            for (int i = 0; i < jobCount; i++) {
                val = (rnd.nextInt(4) + 1) * 10;
                totalCount += multiplier * val;
                jobs.add(s.submitJob(val));
            }

            // client goes offline
            try {
                synchronized (this) {
                    this.wait(totalCount / 4);
                }
                failingClient.stopService();
            } catch (InterruptedException ex) {
                fail("Failed to wait before client goes offline.");
            }

            s.getJobManager().waitForAllJobs();

            for (Job j : jobs) {
                counter.add((Integer) j.getResult(true));
            }

            assertEquals(totalCount, counter.getCount());
        } catch (IOException ex) {
            fail("Failed to initialize client.");
        }
    }

    @Test
    public void testCancelFromServer() {
        System.out.println("cancelFromServer");
        final Counter counter = new Counter();

        c.setAssignmentListener(new AssignmentListener() {
            boolean run = true;

            @Override
            public void receiveTask(Assignment task) {
                try {
                    Object tsk = task.getTask();

                    if (!(tsk instanceof Integer)) {
                        fail("Illegal task received");
                    }

                    Integer count = (Integer) tsk;

                    if (!run) {
                        return;
                    }

                    synchronized (JobTest.this) {
                        JobTest.this.wait(count);
                    }

                    if (!run) {
                        return;
                    }

                    counter.add(count);
                    task.submitResult(GenericResponses.OK);
                } catch (ConnectionException ex) {
                    fail("Connection to server failed - " + ex);
                } catch (InterruptedException ex) {
                    fail("Waiting has been interrupted - " + ex);
                }
            }

            @Override
            public void cancelTask(Assignment task) {
                run = false;
            }
        });

        int cnt = 0, val;
        Random rnd = new Random();
        final int jobCount = rnd.nextInt(5) + 5;
        Set<Job> jobs = new HashSet<Job>(jobCount);
        for (int i = 0; i < jobCount; i++) {
            val = (rnd.nextInt(4) + 2) * 20;
            cnt += val;
            jobs.add(s.submitJob(val));
        }
        Job jobForCancelation = s.submitJob(250);

        while (!JobStatus.ACCEPTED.equals(jobForCancelation.getStatus())) {
            try {
                synchronized (this) {
                    this.wait(10);
                }
            } catch (InterruptedException ex) {
                fail("Waiting for cancelation failed.");
            }
        }
        try {
            jobForCancelation.cancelJob();
        } catch (ConnectionException ex) {
            fail("Failed to cancel the job.");
        }

        s.getJobManager().waitForAllJobs();

        for (Job j : jobs) {
            assertEquals(GenericResponses.OK, j.getResult(true));
        }

        assertEquals(cnt, counter.getCount());
    }

    @Test
    public void testCancelAllJobs() {
        System.out.println("cancelAllJobs");
        final Counter counter = new Counter();

        c.setAssignmentListener(new AssignmentListener() {
            boolean run = true;

            @Override
            public void receiveTask(Assignment task) {
                try {
                    Object tsk = task.getTask();

                    if (!(tsk instanceof Integer)) {
                        fail("Illegal task received");
                    }

                    Integer count = (Integer) tsk;

                    if (!run) {
                        return;
                    }

                    synchronized (JobTest.this) {
                        JobTest.this.wait(count);
                    }

                    if (!run) {
                        return;
                    }

                    counter.add(count);
                    task.submitResult(GenericResponses.OK);
                } catch (ConnectionException ex) {
                    fail("Connection to server failed - " + ex);
                } catch (InterruptedException ex) {
                    fail("Waiting has been interrupted - " + ex);
                }
            }

            @Override
            public void cancelTask(Assignment task) {
                run = false;
            }
        });

        int val;
        Random rnd = new Random();
        final int jobCount = rnd.nextInt(5) + 5;
        Set<Job> jobs = new HashSet<Job>(jobCount);
        for (int i = 0; i < jobCount; i++) {
            val = (rnd.nextInt(4) + 1) * 200;
            jobs.add(s.submitJob(val));
        }

        try {
            synchronized (this) {
                this.wait(10);
            }
        } catch (InterruptedException ex) {
            fail("Waiting for cancelation failed.");
        }

        System.out.println("Submit");
        s.getJobManager().stopAllJobs();
        System.out.println("Stop");
        for (Job j : jobs) {
            assertEquals(null, j.getResult(true));
        }
        System.out.println("Result");
        assertEquals(0, counter.getCount());
        System.out.println("Counter");
    }

    private final class Counter {

        int count;
        int max;

        public Counter() {
            count = 0;
            checkMax();
        }

        public synchronized void add(final int count) {
            this.count += count;
            checkMax();
        }

        public synchronized void sub(final int count) {
            this.count -= count;
        }

        private void checkMax() {
            if (count > max) {
                max = count;
            }
        }

        public synchronized int getCount() {
            return this.count;
        }

        public int getMax() {
            return max;
        }
    }
}