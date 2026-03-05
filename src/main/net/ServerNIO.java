import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.net.InetSocketAddress;
import java.util.Iterator;


public class ServerNIO {
    private static final int PORT = 3000;

    public void start() {

        try (Selector selector = Selector.open();
            ServerSocketChannel serverChannel = ServerSocketChannel.open()) {

            serverChannel.bind(new InetSocketAddress(PORT));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("NIO server started on port " + PORT);

            while (true) {
                selector.select();
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {

                    SelectionKey key = keys.next();
                    keys.remove();

                    if (key.isAcceptable()) {
                        handleAccept(serverChannel, selector);
                    }

                    if(key.isReadable()) {
                        handleRead(key);
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private void handleAccept(ServerSocketChannel serverChannel, Selector selector) throws IOException {

        SocketChannel client = serverChannel.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);

        System.out.println("Client connected: " + client.getRemoteAddress());
    }

    private void handleRead(SelectionKey key) throws IOException {

        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = client.read(buffer);
        System.out.println("Read bytes: " + bytesRead);

        if (bytesRead == -1) {
            client.close();
            System.out.println("Client disconnected");
            return;
        }

        buffer.flip();
        client.write(buffer);

    }

    public static void main(String[] args) {
        ServerNIO s = new ServerNIO();
        s.start();
    }
}
