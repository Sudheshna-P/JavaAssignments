import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.List;

import logger.Logger;
import logger.LoggerManager;
import logger.LoggerFactory;

public class SimpleHttpServer {

    private static final int PORT = 8080;
    private static final String BASE = "/home/sudheshna/IdeaProjects/JavaAssignments/src";

    private static final Logger fileLogger = LoggerFactory.getFileLogger(
            "/home/sudheshna/IdeaProjects/JavaAssignments/src/main/ioOutput/server.log");
    private static final Logger consoleLogger = LoggerFactory.getConsoleLogger();
    private static final LoggerManager logger = new LoggerManager(List.of(fileLogger, consoleLogger));

    private static void handleClient(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        int bytesRead = client.read(buffer);

        if (bytesRead == -1) {
            client.close();
            return;
        }

        buffer.flip();
        StringBuilder requestBuilder = (StringBuilder) key.attachment();
        requestBuilder.append(new String(buffer.array(), 0, bytesRead));

        String request = requestBuilder.toString();
        if (!request.contains("\r\n\r\n")) return;

        String requestLine = request.lines().findFirst().orElse("");
        if (requestLine.isEmpty()) {
            logger.debug("Empty request received");
            client.close();
            return;
        }

        logger.info("Request: " + requestLine);

        String[] parts = requestLine.split(" ");
        String method = parts[0];
        String path   = parts[1];

        if (!method.equals("GET")) {
            logger.info("Method not allowed: " + method + " " + path);
            client.write(ByteBuffer.wrap(send405().getBytes()));
            client.close();
            return;
        }

        if (path.equals("/")) path = "/index.html";

        String folder = "Public", prefix = "";
        if      (path.startsWith("/images")) { folder = "images";    prefix = "/images"; }
        else if (path.startsWith("/files"))  { folder = "documents"; prefix = "/files";  }

        String relativePath = prefix.isEmpty() ? path : path.substring(prefix.length());
        if (relativePath.isEmpty() || relativePath.equals("/")) {
            client.write(ByteBuffer.wrap(send404().getBytes()));
            client.close();
            return;
        }

        Path filePath = Paths.get(BASE, folder, relativePath.substring(1));

        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            logger.info("File not found: " + filePath.toAbsolutePath());
            client.write(ByteBuffer.wrap(send404().getBytes()));
            client.close();
            return;
        }

        long fileSize = Files.size(filePath);
        String contentType = getContentType(path);
        logger.debug("Serving: " + filePath + " size=" + fileSize + " type=" + contentType);

        client.write(ByteBuffer.wrap(sendHeader(fileSize, contentType).getBytes()));

        try (FileChannel fileChannel = FileChannel.open(filePath)) {
            fileChannel.transferTo(0, fileSize, client);
        }

        client.close();
        logger.info("Served: " + path + " (" + fileSize + " bytes)");
    }

    private static String sendHeader(long length, String type) {
        return "HTTP/1.1 200 OK\r\n"
                + "Content-Type: "   + type   + "\r\n"
                + "Content-Length: " + length + "\r\n"
                + "Connection: close\r\n\r\n";
    }

    private static String send404() {
        String body = "<h1>404 Not Found</h1>";
        return "HTTP/1.1 404 Not Found\r\n"
                + "Content-Type: text/html\r\n"
                + "Content-Length: " + body.length() + "\r\n\r\n" + body;
    }

    private static String send405() {
        String body = "<h1>405 Method Not Allowed</h1>";
        return "HTTP/1.1 405 Method Not Allowed\r\n"
                + "Content-Type: text/html\r\n"
                + "Content-Length: " + body.length() + "\r\n\r\n" + body;
    }

    private static String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".css"))  return "text/css";
        if (path.endsWith(".js"))   return "application/javascript";
        if (path.endsWith(".png"))  return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".gif"))  return "image/gif";
        if (path.endsWith(".txt"))  return "text/plain";
        return "application/octet-stream";
    }

    public static void main(String[] args) {
        logger.info("Starting HTTP server on port " + PORT);
        new ServerNIO(PORT, SimpleHttpServer::handleClient).start(); // just like passing loggers in
    }
}