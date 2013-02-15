package cz.tul.comm.client;

import cz.tul.comm.socket.Communicator;
import cz.tul.comm.socket.IMessageHandler;
import cz.tul.comm.IService;
import cz.tul.comm.SerializationUtils;
import cz.tul.comm.server.Comm_Server;
import cz.tul.comm.socket.ServerSocket;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class Comm_Client implements IService {

    public static final int PORT = 5253;
    private final ServerSocket serverSocket;
    private final Communicator comm;
    private final Settings settings;

    private Comm_Client() {
        ServerSocket tmp = null;
        try {
            tmp = new ServerSocket(PORT);
        } catch (IOException ex) {
            // TODO logging
            Logger.getLogger(Comm_Server.class.getName()).log(Level.SEVERE, null, ex);
            // TODO handle that socket cannot be bound
        }
        serverSocket = tmp;

        File s = new File(Settings.SERIALIZATION_NAME);
        if (s.exists()) {
            Object in = SerializationUtils.loadItemFromDisc(s, true);
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
        SerializationUtils.saveItemToDisc(new File(Settings.SERIALIZATION_NAME), settings, true);
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
