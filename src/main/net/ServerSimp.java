import java.io.*;
import java.net.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ServerSimp {

    private static final int PORT = 3000;
    private static final Logger logger =
            Logger.getLogger(ServerSimp.class.getName());

    public static void main(String[] args) {
        new ServerSimp().start();
    }

    public void start() {
        logger.info("Server started on port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            while (true) {
                Socket socket = serverSocket.accept();
                logger.info("Client connected: " + socket.getInetAddress());

                handleClient(socket);
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Server error", e);
        }
    }

    private void handleClient(Socket socket) {

        try (socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()));
             BufferedWriter out = new BufferedWriter(
                     new OutputStreamWriter(socket.getOutputStream()))) {

            String message;

            while ((message = in.readLine()) != null) {

                if ("exit".equalsIgnoreCase(message)) {
                    break;
                }

                logger.info("Received: " + message);

                out.write("Echo: " + message);
                out.newLine();
                out.flush();
            }

        } catch (IOException e) {
            logger.log(Level.WARNING, "Client error", e);
        }

        logger.info("Client disconnected");
    }
}