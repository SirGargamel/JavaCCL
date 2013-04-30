package cz.tul.comm.tester;

import cz.tul.comm.ComponentSwitches;
import cz.tul.comm.Constants;
import cz.tul.comm.Utils;
import cz.tul.comm.client.Client;
import cz.tul.comm.client.ClientImpl;
import cz.tul.comm.client.ServerInterface;
import cz.tul.comm.communicator.Communicator;
import cz.tul.comm.communicator.CommunicatorImpl;
import cz.tul.comm.history.History;
import cz.tul.comm.history.HistoryManager;
import cz.tul.comm.history.sorting.IPSorter;
import cz.tul.comm.history.sorting.InOutSorter;
import cz.tul.comm.history.sorting.TimeSorter;
import cz.tul.comm.job.Assignment;
import cz.tul.comm.job.AssignmentListener;
import cz.tul.comm.job.Job;
import cz.tul.comm.messaging.BasicConversator;
import cz.tul.comm.messaging.Message;
import cz.tul.comm.persistence.ClientSettings;
import cz.tul.comm.persistence.ServerSettings;
import cz.tul.comm.server.ClientManager;
import cz.tul.comm.server.ServerImpl;
import cz.tul.comm.server.DataStorage;
import cz.tul.comm.server.Server;
import cz.tul.comm.socket.queue.Identifiable;
import cz.tul.comm.socket.queue.Listener;
import cz.tul.comm.tester.virtual.DummyClient;
import cz.tul.comm.tester.virtual.DummyServer;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * class for testing.
 *
 * @author Petr Ječmen
 */
public class Main {

    private static final Logger log = Logger.getLogger(Main.class.getName());
    private static final double CLIENT_ERROR_CHANCE_MAX = 0.1;
    private static final double CLIENT_FATAL_CHANCE_MAX = 0.05;
    private static Server s;
    private static Client c;

    /**
     * @param args the command line arguments
     * @throws UnknownHostException
     * @throws InterruptedException
     */
    public static void main(String[] args) throws UnknownHostException, InterruptedException {
//        basicTesting(args);
        testWithDummies(args);
    }

    private static void basicTesting(String[] args) throws SecurityException {
        System.out.println("... Init ...");
        Level l = Level.INFO;

        for (String arg : args) {
            switch (arg) {
                case "-cdd":
                    ComponentSwitches.useClientDiscovery = false;
                    break;
                case "-settings":
                    ComponentSwitches.useSettings = false;
                    break;
                case "-csd":
                    ComponentSwitches.useClientStatus = false;
                    break;
                case "-d":
                    l = Level.CONFIG;
                    break;
                case "-d2":
                    l = Level.FINE;
                    break;
            }
        }

        log.getParent().setLevel(l);
        for (Handler h : log.getParent().getHandlers()) {
            h.setLevel(l);
        }

        s = ServerImpl.initNewServer();
        c = ClientImpl.initNewClient(5_253);
        c.registerToServer(InetAddress.getLoopbackAddress(), Constants.DEFAULT_PORT);

        System.out.println("... Testing ...");
        settingsTest();

        System.out.println("... Shutdown started ...");
        s.stopService();
        c.stopService();
    }

    static void historyTest() throws UnknownHostException, InterruptedException {
        HistoryManager hm = new History();

        Message m1 = new Message("h1", "d1");
        Message m2 = new Message("h2", "d2");

        InetAddress ip = InetAddress.getByAddress(new byte[]{1, 1, 1, 1});
        InetAddress ip2 = InetAddress.getByAddress(new byte[]{2, 1, 1, 1});
        InetAddress ip3 = InetAddress.getByAddress(new byte[]{3, 1, 1, 1});

        hm.logMessageReceived(ip2, m2, true, null);
        Thread.sleep(100);
        hm.logMessageSend(ip2, m1, true, null);
        hm.logMessageReceived(ip, m2, true, null);
        Thread.sleep(150);
        hm.logMessageReceived(ip3, m2, true, null);
        Thread.sleep(200);
        hm.logMessageSend(ip3, m1, true, null);

        hm.export(new File("testIP.xml"), new IPSorter(true));
        hm.export(new File("testInOut.xml"), new InOutSorter());
        hm.export(new File("testTime.xml"), new TimeSorter());
        hm.export(new File("test.xml"), null);
    }

    static void systemMessageTest() throws InterruptedException {
        final int PORT = 5_254;

        Client c2 = ClientImpl.initNewClient(PORT);

        s.getClientManager().registerClient(InetAddress.getLoopbackAddress(), PORT);

        Object mon = new Object();
        synchronized (mon) {
            mon.wait(10_000);
        }

        c2.stopService();
    }

    static void discoveryDaemonTesting(final String[] args) throws InterruptedException {
        log.getParent().setLevel(Level.CONFIG);
        for (Handler h : log.getParent().getHandlers()) {
            h.setLevel(Level.CONFIG);
        }

        if (args.length > 0) {
            for (String arg : args) {
                switch (arg) {
                    case "-c":
                        c = ClientImpl.initNewClient();
                        break;
                    case "-s":
                        s = ServerImpl.initNewServer();
                        break;
                    default:
                        break;
                }
            }
        } else {
            System.out.println("Usage:  DIC_Comm.jar [-c] [-s]");
            System.out.println("");
            System.out.println("Aviable switches - ");
            System.out.println("    -s      Create and run instance of server");
            System.out.println("    -c      Create and run instance of client");
        }
    }

    static void loginTest() throws InterruptedException {
        c.registerToServer(InetAddress.getLoopbackAddress(), Constants.DEFAULT_PORT);

        Thread.sleep(20_000);
    }

    static void responderTest() throws InterruptedException {
        final int PORT_C = 5_253;

        final UUID id = UUID.randomUUID();
        final Message mOut = new Message(id, "fromServer", null);
        final Message mIn = new Message(id, "fromClient", null);

        c.registerToServer(InetAddress.getLoopbackAddress(), Constants.DEFAULT_PORT);
        c.getListenerRegistrator().setIdListener(id, new Listener() {
            @Override
            public Object receiveData(Identifiable data) {
                if (data instanceof Message) {
                    final Message m = (Message) data;
                    if (m.getHeader().equals("fromServer")) {
                        System.out.println("Data received - " + data.toString());
                        return mIn;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }, true);


        Communicator comm = s.getClientManager().registerClient(InetAddress.getLoopbackAddress(), PORT_C);

        final BasicConversator r = new BasicConversator(comm, s.getListenerRegistrator());
        System.out.println("Response is " + r.sendAndReceiveData(mOut));
    }

    static void jobTest() {
        final Object task = "jobAssignment";
        final Object result = "jobResult";

        DataStorage ds = new DataStorage() {
            @Override
            public Object requestData(Object dataId) {
                return dataId;
            }
        };
        s.assignDataStorage(ds);
        System.out.println("Data storage created and assigned to server.");

        c.setAssignmentListener(new AssignmentListener() {
            @Override
            public void receiveTask(Assignment task) {
                System.out.println("Received task - " + task.getTask().toString());
                final String dataRequest = "data";
                System.out.println("Sending data request.");
                final Object dataFromServer = task.requestData(dataRequest);
                if (!dataFromServer.equals(dataRequest)) {
                    System.err.println("Invalid data received from server - " + dataFromServer.toString());
                } else {
                    System.out.println("Data received successfully, waiting for task to finish.");
                }
                synchronized (Main.class) {
                    try {
                        Main.class.wait(2_000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                System.out.println("Sending result.");
                task.submitResult(result);
            }

            @Override
            public void cancelTask(Assignment taskId) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });
        System.out.println("Task listener created and assigned to client.");

        final Job job = s.submitJob(task);
        System.out.println("Job submitted, waiting for result.");
        final Object jobResult = job.getResult(true);
        if (!jobResult.equals(result)) {
            System.err.println("Results do not match - " + result + "vs " + jobResult);
        } else {
            System.out.println("Result received and matches input.");
        }
    }

    static void settingsTest() {
        ClientManager cl = new ClientManager() {
            @Override
            public Communicator registerClient(InetAddress adress, int port) {
                System.out.println("Registering client " + adress + ":" + port);
                return null;
            }

            @Override
            public void deregisterClient(UUID id) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Communicator getClient(InetAddress adress, int port) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Communicator getClient(UUID id) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Set<Communicator> getClients() {
                final Set<Communicator> comms = new HashSet<>();

                comms.add(CommunicatorImpl.initNewCommunicator(InetAddress.getLoopbackAddress(), 50));
                comms.add(CommunicatorImpl.initNewCommunicator(InetAddress.getLoopbackAddress(), 51));

                return comms;
            }
        };
        ServerSettings.serialize(cl);
        ServerSettings.deserialize(cl);

        ServerInterface si = new ServerInterface() {
            @Override
            public boolean registerToServer(InetAddress address, int port) {
                System.out.println("Registering server " + address + ":" + port);
                return true;
            }

            @Override
            public void deregisterFromServer() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public boolean isServerUp() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Communicator getServerComm() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public int getLocalSocketPort() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
        Communicator comm = CommunicatorImpl.initNewCommunicator(InetAddress.getLoopbackAddress(), 52);
        ClientSettings.serialize(comm);
        ClientSettings.deserialize(si);
    }

    static void testWithDummies(String[] args) throws UnknownHostException {
        Utils.adjustMainLoggerLevel(Level.CONFIG);

        boolean interactiveMode = false;
        DummyServer srv = null;
        DummyClient dc = null;
        int repCount;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-c":
                    dc = DummyClient.newInstance(CLIENT_ERROR_CHANCE_MAX * Math.random(), CLIENT_FATAL_CHANCE_MAX * Math.random());
                    break;
                case "-s":
                    srv = new DummyServer();
                    break;
                case "-i":
                    interactiveMode = true;
                    break;
                case "-j":
                    try {
                        repCount = Integer.parseInt(args[i + 1]);
                        if (srv != null) {
                            srv.submitJob(repCount);
                        } else {
                            log.warning("Server not initialized yet, commands must be after -s modifier.");
                        }
                    } catch (NumberFormatException ex) {
                        log.log(Level.WARNING, "Illegal count of repetitions used - {0}", args[i + 1]);
                    }

                    i++;
                    break;
            }
        }

        if (srv != null || dc != null) {
            if (interactiveMode) {
                System.out.println("Starting interactive mode.");
                System.out.println("Input commands with parameters, one command per line, parameters are separated by spaces.");

                String line;
                Scanner in = new Scanner(System.in);
                boolean run = true;
                String[] split;
                while (run) {
                    line = in.nextLine().trim().replaceAll("[ ]+", " ");
                    split = line.split(" ");
                    if (split.length > 0) {
                        switch (split[0]) {
                            case "j":
                                if (split.length > 1 && srv != null) {
                                    try {
                                        repCount = Integer.parseInt(split[1]);
                                        srv.submitJob(repCount);
                                    } catch (NumberFormatException ex) {
                                        log.log(Level.WARNING, "Illegal count of repetitions used - {0}", split[1]);
                                    }
                                } else {
                                    log.warning("Job needs repetition count as parameter.");
                                }
                                break;
                            case "cs":
                                if (split.length > 1 && dc != null) {
                                    dc.connectToServer(split[1]);
                                }
                            case "ca":
                                if (split.length > 1 && srv != null) {
                                    srv.registerClient(split[1]);
                                }
                                break;
                            case "c":
                                dc = DummyClient.newInstance(CLIENT_ERROR_CHANCE_MAX * Math.random(), CLIENT_FATAL_CHANCE_MAX * Math.random());
                                break;
                            case "s":
                                srv = new DummyServer();
                                break;
                            default:
                                System.out.println("Illegal input !");
                                break;
                        }
                    }
                }
            } else if (srv != null) {
                srv.waitForJobs();
            }
        }
    }
}
