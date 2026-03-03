import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClientSimp {

    private static final String HOST = "localhost";
    private static final int PORT = 3000;

    public static void main(String[] args) {

        System.out.println("Connecting to server...");

        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);  // auto flush
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to server.");
            System.out.println("Type 'exit' to quit.");

            while (true) {

                String message = scanner.nextLine();

                out.println(message);

                if ("exit".equalsIgnoreCase(message)) {
                    break;
                }

                String response = in.readLine();  // read server reply
                System.out.println("Server replied: " + response);
            }

            System.out.println("Disconnected from server.");

        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}