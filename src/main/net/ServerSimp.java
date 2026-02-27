import java.io.*;
import java.net.*;

public class ServerSimp {

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(3000);
            System.out.println("Server started. Waiting for client...");

            while (!serverSocket.isClosed()) {

                Socket socket = serverSocket.accept();
                System.out.println("Client connected");

                try (
                        Socket autoSocket = socket;
                        DataInputStream in = new DataInputStream(autoSocket.getInputStream());
                        DataOutputStream out = new DataOutputStream(autoSocket.getOutputStream())
                ) {

                    while (true) {
                        try {
                            String message = in.readUTF();
                            System.out.println("Received: " + message);
                            out.writeUTF("Echo: " + message);
                            out.flush();
                        } catch (EOFException e) {
                            break;
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.out.println("Client disconnected");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}