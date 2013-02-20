package cz.tul.comm.client;

import cz.tul.comm.socket.Communicator;
import cz.tul.comm.socket.IMessageHandler;
import cz.tul.comm.IService;
import cz.tul.comm.SerializationUtils;
import cz.tul.comm.gui.UserLogging;
import cz.tul.comm.socket.ServerSocket;
import java.io.File;
import java.net.BindException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class enclosing client to server communication. Allows data sending
 * and has a registry for message handling.
 * @author Petr Ječmen
 */
public class Comm_Client implements IService {

    private static final Logger log = Logger.getLogger(Comm_Client.class.getName());
    public static final int PORT = 5253;
    private final ServerSocket serverSocket;
    private final Communicator comm;
    private final Settings settings;

    private Comm_Client() {
        ServerSocket tmp = null;
        try {
            tmp = new ServerSocket(PORT);
        } catch (BindException ex) {
            UserLogging.showErrorToUser("Error creating server socket on port" + PORT);
        }
        serverSocket = tmp;

        File s = new File(Settings.SERIALIZATION_NAME);
        if (s.exists()) {
            Object in = SerializationUtils.loadXMLItemFromDisc(s);
            if (in instanceof Settings) {
                settings = (Settings) in;
            } else {
                settings = new Settings();
                settings.setServerAdress(InetAddress.getLoopbackAddress().getHostAddress());
            }
        } else {
            settings = new Settings();
            settings.setServerAdress(InetAddress.getLoopbackAddress().getHostAddress());
        }
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                saveData();
            }
        }));


        Communicator c = null;
        try {
            InetAddress serverIp = InetAddress.getByName(settings.getServerAdress());
            c = new Communicator(serverIp, settings.getServerPort());
        } catch (UnknownHostException ex) {
            UserLogging.showWarningToUser("Unknown host found in settings - " + settings.getServerAdress());
            log.log(Level.WARNING, "Unkonwn host found in settings", ex);
        }
        comm = c;
    }

    public void addMessageHandler(final IMessageHandler msgHandler) {
        serverSocket.addMessageHandler(msgHandler);
    }

    public void sendMessage(final Object data) {
        comm.sendData(data);
    }

    public void saveData() {
        SerializationUtils.saveItemToDiscAsXML(new File(Settings.SERIALIZATION_NAME), settings);
    }

    public static Comm_Client initNewClient() {
        final Comm_Client result = new Comm_Client();

        result.start();

        return result;
    }

    void start() {
        serverSocket.start();
    }

    @Override
    public void stopService() {
        serverSocket.stopService();
    }
}
