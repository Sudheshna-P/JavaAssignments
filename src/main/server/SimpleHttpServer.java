import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import logger.Logger;
import logger.LoggerManager;
import logger.LoggerFactory;
public class SimpleHttpServer {

    private static final int PORT = 8080;
    private static final String BASE = "/home/sudheshna/IdeaProjects/JavaAssignments/src";

    private static final DateTimeFormatter HTTP_DATE =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
                    .withZone(ZoneOffset.UTC);

    private final LoggerManager logger;

    public SimpleHttpServer() {
        Logger fileLogger = LoggerFactory.getFileLogger(
                "/home/sudheshna/IdeaProjects/JavaAssignments/src/main/ioOutput/server.log");
        Logger consoleLogger = LoggerFactory.getConsoleLogger();
        this.logger = new LoggerManager(List.of(fileLogger, consoleLogger));
    }

    private void handleClient(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();

        ByteBuffer buffer = ByteBuffer.allocate(8192);
        int bytesRead = client.read(buffer);
        if (bytesRead == -1) {
            client.close();
            return;
        }
        buffer.flip();

        ByteArrayOutputStream acc = (ByteArrayOutputStream) key.attachment();
        if (acc == null) {
            acc = new ByteArrayOutputStream();
            key.attach(acc);
        }

        acc.write(buffer.array(), 0, buffer.limit());
        byte[] data = acc.toByteArray();

        int processed = 0;

        while (true) {
            int headerEnd = findHeaderEnd(data, processed);
            if (headerEnd == -1) break;

            int end = headerEnd + 4;

            String headers = new String(data, processed, end - processed, StandardCharsets.US_ASCII);
            String requestLine = headers.lines().findFirst().orElse("");

            if (requestLine.isEmpty()) {
                client.close();
                return;
            }

            logger.info("Request: " + requestLine);

            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                client.close();
                return;
            }

            String method = parts[0];
            String path = parts[1];

            String connectionHeader = extractHeader(headers, "Connection");
            boolean keepAlive = connectionHeader == null || !connectionHeader.equalsIgnoreCase("close");

            if (method.equals("POST")) {

                String contentLengthHeader = extractHeader(headers, "Content-Length");
                int contentLength = contentLengthHeader != null
                        ? Integer.parseInt(contentLengthHeader.trim())
                        : 0;

                if (data.length < end + contentLength) {
                    break;
                }

                String body = new String(
                        data,
                        end,
                        contentLength,
                        StandardCharsets.UTF_8
                );

                logger.info("POST Body: " + body);

                // Simple response
                String responseBody = "<h1>POST Received</h1>";

                String response =
                        "HTTP/1.1 200 OK\r\n" +
                                "Date: " + HTTP_DATE.format(ZonedDateTime.now()) + "\r\n" +
                                "Content-Type: text/html\r\n" +
                                "Content-Length: " + responseBody.length() + "\r\n" +
                                "Connection: " + (keepAlive ? "keep-alive" : "close") + "\r\n\r\n" +
                                responseBody;

                client.write(ByteBuffer.wrap(response.getBytes()));

                if (!keepAlive) {
                    client.close();
                    return;
                }

                processed = end + contentLength;
                continue;
            }
            else if (!method.equals("GET")) {
                client.write(ByteBuffer.wrap(send405().getBytes()));
                if (!keepAlive) client.close();
                processed = end;
                continue;
            }

            if (path.equals("/")) path = "/index.html";

            String folder = "Public", prefix = "";
            if (path.startsWith("/images")) { folder = "images"; prefix = "/images"; }
            else if (path.startsWith("/files")) { folder = "documents"; prefix = "/files"; }

            String relativePath = prefix.isEmpty() ? path : path.substring(prefix.length());
            if (relativePath.isEmpty() || relativePath.equals("/")) {
                client.write(ByteBuffer.wrap(send404().getBytes()));
                if (!keepAlive) client.close();
                processed = end;
                continue;
            }

            Path filePath = Paths.get(BASE, folder, relativePath.substring(1));
            Path normalized = filePath.normalize();
            Path basePath = Paths.get(BASE).toAbsolutePath();

            if (!normalized.toAbsolutePath().startsWith(basePath)) {
                String response = "HTTP/1.1 403 Forbidden\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n\r\n";

                client.write(ByteBuffer.wrap(response.getBytes()));
                client.close();
                return;
            }

            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                client.write(ByteBuffer.wrap(send404().getBytes()));
                if (!keepAlive) client.close();
                processed = end;
                continue;
            }

            long fileSize = Files.size(filePath);
            String contentType = getContentType(path);
            String lastModified = HTTP_DATE.format(Files.getLastModifiedTime(filePath).toInstant());
            String date = HTTP_DATE.format(ZonedDateTime.now());

            String ifModSince = extractHeader(headers, "If-Modified-Since");
            if (ifModSince != null) {
                try {
                    ZonedDateTime clientTime = ZonedDateTime.parse(ifModSince, HTTP_DATE);
                    if (!Files.getLastModifiedTime(filePath).toInstant().isAfter(clientTime.toInstant())) {

                        String response = "HTTP/1.1 304 Not Modified\r\n" +
                                          "Date: " + date + "\r\n" +
                                          "Last-Modified: " + lastModified + "\r\n" +
                                          "Connection: " + (keepAlive ? "keep-alive" : "close") + "\r\n\r\n";

                        client.write(ByteBuffer.wrap(response.getBytes()));
                        if (!keepAlive) client.close();
                        processed = end;
                        continue;
                    }
                } catch (Exception ignored) {}
            }

            String rangeHeader = extractHeader(headers, "Range");
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {

                long start = 0, endByte = fileSize - 1;

                try {
                    String[] range = rangeHeader.substring(6).split("-");
                    if (!range[0].isEmpty()) start = Long.parseLong(range[0]);
                    if (range.length > 1 && !range[1].isEmpty()) endByte = Long.parseLong(range[1]);
                } catch (Exception e) {
                    start = 0;
                    endByte = fileSize - 1;
                }

                if (start > endByte || start >= fileSize) {
                    client.write(ByteBuffer.wrap(("HTTP/1.1 416 Range Not Satisfiable\r\n\r\n").getBytes()));
                    if (!keepAlive) client.close();
                    processed = end;
                    continue;
                }

                long contentLength = endByte - start + 1;

                String header = "HTTP/1.1 206 Partial Content\r\n" +
                                "Date: " + date + "\r\n" +
                                "Content-Type: " + contentType + "\r\n" +
                                "Content-Length: " + contentLength + "\r\n" +
                                "Content-Range: bytes " + start + "-" + endByte + "/" + fileSize + "\r\n" +
                                "Last-Modified: " + lastModified + "\r\n" +
                                "Connection: " + (keepAlive ? "keep-alive" : "close") + "\r\n\r\n";

                client.write(ByteBuffer.wrap(header.getBytes()));

                try (FileChannel fc = FileChannel.open(filePath)) {
                    fc.transferTo(start, contentLength, client);
                }

                logger.info("206 Served: " + path + " (" + start + "-" + endByte + ")");
            } else {
                String header = "HTTP/1.1 200 OK\r\n" +
                                "Date: " + date + "\r\n" +
                                "Content-Type: " + contentType + "\r\n" +
                                "Content-Length: " + fileSize + "\r\n" +
                                "Last-Modified: " + lastModified + "\r\n" +
                                "Connection: " + (keepAlive ? "keep-alive" : "close") + "\r\n\r\n";

                client.write(ByteBuffer.wrap(header.getBytes()));

                try (FileChannel fc = FileChannel.open(filePath)) {
                    fc.transferTo(0, fileSize, client);
                }

                logger.info("200 Served: " + path);
            }

            if (!keepAlive) {
                client.close();
                return;
            }

            processed = end;
        }

        ByteArrayOutputStream newAcc = new ByteArrayOutputStream();
        newAcc.write(data, processed, data.length - processed);
        key.attach(newAcc);

        key.interestOps(SelectionKey.OP_READ);
    }

    private static int findHeaderEnd(byte[] data, int start) {
        for (int i = start; i < data.length - 3; i++) {
            if (data[i] == 13 && data[i+1] == 10 && data[i+2] == 13 && data[i+3] == 10)
                return i;
        }
        return -1;
    }

    private static String extractHeader(String headers, String name) {
        String search = "\r\n" + name + ":";
        int i = headers.indexOf(search);
        if (i == -1) return null;
        int start = i + search.length();
        int end = headers.indexOf("\r\n", start);
        return end == -1 ? null : headers.substring(start, end).trim();
    }

    private static String send404() {
        String body = "<h1>404 Not Found</h1>";
        return "HTTP/1.1 404 Not Found\r\nContent-Length: " + body.length() + "\r\n\r\n" + body;
    }

    private static String send405() {
        String body = "<h1>405 Method Not Allowed</h1>";
        return "HTTP/1.1 405 Method Not Allowed\r\nContent-Length: " + body.length() + "\r\n\r\n" + body;
    }

    private static String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".gif")) return "image/gif";
        return "application/octet-stream";
    }

    public static void main(String[] args) {
        SimpleHttpServer server = new SimpleHttpServer();
        server.logger.info("Starting server on port " + PORT);
        new ServerNIO(PORT, server::handleClient).start();
    }
}