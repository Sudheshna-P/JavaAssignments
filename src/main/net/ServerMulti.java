import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerMulti {

    private static final int port = 3000;
    private static final Logger logger = Logger.getLogger(ServerMulti.class.getName());

    public void start() {
        logger.info("Multi-threaded Server started on port: " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("Client connected: " + clientSocket.getInetAddress());

                Thread thread = new Thread(new ClientHandler(clientSocket));
                thread.start();
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Server error", e);
        }
    }

    public static void main(String[] args) {
        new ServerMulti().start();
    }
}