import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClientSimp {

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 3000);

            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            Scanner scanner = new Scanner(System.in);

            System.out.println("Connected to server. Type messages (type 'exit' to quit):");

            while (true) {
                String message = scanner.nextLine();
                if (message.equalsIgnoreCase("exit")) {
                    break;
                }

                out.writeUTF(message);
                out.flush();

                String response = in.readUTF();
                System.out.println("Server replied: " + response);
            }

            socket.close();
            scanner.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}