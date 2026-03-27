import java.io.*;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ClientHandler implements Runnable {

    private final Socket socket;
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        handleClient();
    }

    private void handleClient() {

        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            String message;
            while ((message = in.readLine()) != null) {

                if ("exit".equalsIgnoreCase(message)) {
                    break;
                }

                logger.info("Received from " + socket.getInetAddress() + ": " + message);

                out.write("Echo: " + message);
                out.newLine();
                out.flush();
            }

        } catch (IOException e) {
            logger.log(Level.WARNING, "Client error", e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error closing socket", e);
            }

            logger.info("Client disconnected: " + socket.getInetAddress());
        }
    }
}
