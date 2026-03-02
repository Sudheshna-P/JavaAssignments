import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClientSimp {

    public static void main(String[] args) {

        System.out.println("Connecting to server...");

        try (Socket socket = new Socket("localhost", 3000);
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to server.");
            System.out.println("Type 'exit' to quit");

            while (true) {

                String message = scanner.nextLine();

                out.writeUTF(message);
                out.flush();

                if (message.equalsIgnoreCase("exit")) {
                    break;
                }
                String response = in.readUTF();
                System.out.println("Server replied: " + response);
            }

            System.out.println("Disconnected from server.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}