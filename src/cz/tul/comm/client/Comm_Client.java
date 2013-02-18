package cz.tul.comm.client;

import cz.tul.comm.socket.Communicator;
import cz.tul.comm.socket.IMessageHandler;
import cz.tul.comm.IService;
import cz.tul.comm.SerializationUtils;
import cz.tul.comm.gui.UserLogging;
import cz.tul.comm.server.Comm_Server;
import cz.tul.comm.socket.ServerSocket;
import java.io.File;
import java.net.BindException;
import java.util.logging.Logger;

/**
 *
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
            }
        } else {
            settings = new Settings();
        }
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                saveData();
            }
        }));

        comm = new Communicator(settings.getServerAdress(), Comm_Server.PORT);
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
