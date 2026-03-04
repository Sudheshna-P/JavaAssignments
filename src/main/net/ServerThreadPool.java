import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerThreadPool {

    private static final int PORT = 3000;
    private static final int POOL_SIZE = 3;
    private static final Logger logger = Logger.getLogger(ServerThreadPool.class.getName());

    private final ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE);

    public void start() {
        logger.info("Thread Pool Server started on port: " + PORT);
        logger.info("Max concurrent clients: " + POOL_SIZE);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("Client connected: " +
                        clientSocket.getInetAddress());

                executor.submit(new ClientHandler(clientSocket));
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Server error", e);
        }
    }

    public static void main(String[] args) {
        ServerThreadPool s = new ServerThreadPool();
        s.start();
    }
}