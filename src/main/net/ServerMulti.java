import java.io.*;
import java.net.*;

public class ServerMulti {
    public static void main(String [] args){

        try (ServerSocket server = new ServerSocket(3000)) {

            while (true) {
                Socket client = server.accept();
                System.out.println("New Client connected: " + client.getInetAddress());
                new Thread(new ClientHandler(client)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static class ClientHandler implements Runnable{

        private final Socket clientSocket;
        public ClientHandler(Socket socket){
            this.clientSocket=socket;
        }
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                String line;
                while ((line = in.readLine()) != null) {
                    out.println(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
