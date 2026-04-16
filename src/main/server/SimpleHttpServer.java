import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

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
    private final Dispatcher dispatcher;

    // ===================== CONSTRUCTOR =====================

    public SimpleHttpServer() {

        Logger fileLogger = LoggerFactory.getFileLogger(BASE + "/main/ioOutput/server.log");
        Logger consoleLogger = LoggerFactory.getConsoleLogger();
        this.logger = new LoggerManager(List.of(fileLogger, consoleLogger));

        Router router = new Router();

        FileController fileController = new FileController();
        UploadController uploadController = new UploadController();

        // Explicit routes
        router.addRoute("GET", "/", fileController::getIndex);
        router.addRoute("GET", "/index.html", fileController::getIndex);
        router.addRoute("POST", "/upload", uploadController::upload);

        this.dispatcher = new Dispatcher(router);
    }

    // ===================== PIPELINE =====================

    static class RequestBuffer {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
    }

    private void handleClient(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();

        ByteBuffer buffer = ByteBuffer.allocate(65536);

        int bytesRead;
        try {
            bytesRead = client.read(buffer);
        } catch (IOException e) {
            cancelAndClose(key, client);
            return;
        }

        if (bytesRead == -1) {
            cancelAndClose(key, client);
            return;
        }

        buffer.flip();

        RequestBuffer acc = getOrCreateBuffer(key);
        acc.buf.write(buffer.array(), 0, buffer.limit());

        int processed = processRequests(client, key, acc);

        handleRemaining(key, acc, processed);
    }

    // ===================== PROCESS =====================

    private int processRequests(SocketChannel client, SelectionKey key, RequestBuffer acc) throws IOException {

        byte[] data = acc.buf.toByteArray();
        int processed = 0;

        while (true) {

            int headerEnd = findHeaderEnd(data, processed);
            if (headerEnd == -1) break;

            int end = headerEnd + 4;

            String headers = new String(data, processed, end - processed, StandardCharsets.US_ASCII);
            String requestLine = headers.lines().findFirst().orElse("");

            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                cancelAndClose(key, client);
                return data.length;
            }

            String method = parts[0];
            String path = parts[1].split("\\?")[0];

            boolean keepAlive = !"close".equalsIgnoreCase(extractHeader(headers, "Connection"));

            HttpRequest request = new HttpRequest();
            request.method = method;
            request.path = path;
            request.headers = parseHeaders(headers);
            request.keepAlive = keepAlive;

            HttpResponse response = dispatcher.dispatch(request);

            client.write(ByteBuffer.wrap(response.toBytes()));

            if (!keepAlive) {
                cancelAndClose(key, client);
                return data.length;
            }

            processed = end;
        }

        return processed;
    }

    // ===================== REMAINING =====================

    private void handleRemaining(SelectionKey key, RequestBuffer acc, int processed) {
        byte[] data = acc.buf.toByteArray();

        RequestBuffer newAcc = new RequestBuffer();

        if (processed < data.length) {
            newAcc.buf.write(data, processed, data.length - processed);
        }

        key.attach(newAcc);
        if (key.isValid()) key.interestOps(SelectionKey.OP_READ);
    }

    private RequestBuffer getOrCreateBuffer(SelectionKey key) {
        Object att = key.attachment();
        return (att instanceof RequestBuffer) ? (RequestBuffer) att : new RequestBuffer();
    }

    // ===================== FRAMEWORK =====================

    static class HttpRequest {
        String method;
        String path;
        Map<String, String> headers;
        boolean keepAlive;
    }

    static class HttpResponse {
        int status = 200;
        String statusText = "OK";
        Map<String, String> headers = new HashMap<>();
        byte[] body = new byte[0];

        public byte[] toBytes() {
            StringBuilder res = new StringBuilder();
            res.append("HTTP/1.1 ").append(status).append(" ").append(statusText).append("\r\n");

            headers.put("Date", HTTP_DATE.format(ZonedDateTime.now()));

            for (var e : headers.entrySet()) {
                res.append(e.getKey()).append(": ").append(e.getValue()).append("\r\n");
            }

            res.append("Content-Length: ").append(body.length).append("\r\n\r\n");

            byte[] head = res.toString().getBytes(StandardCharsets.UTF_8);

            ByteBuffer buffer = ByteBuffer.allocate(head.length + body.length);
            buffer.put(head);
            buffer.put(body);

            return buffer.array();
        }
    }

    @FunctionalInterface
    interface RouteHandler {
        HttpResponse handle(HttpRequest request) throws Exception;
    }

    static class Router {
        private final Map<String, RouteHandler> routes = new HashMap<>();

        void addRoute(String method, String path, RouteHandler handler) {
            routes.put(method + ":" + path, handler);
        }

        RouteHandler find(String method, String path) {
            return routes.get(method + ":" + path);
        }
    }

    // ===================== DISPATCHER =====================

    class Dispatcher {
        private final Router router;

        Dispatcher(Router router) {
            this.router = router;
        }

        HttpResponse dispatch(HttpRequest req) {
            try {
                RouteHandler handler = router.find(req.method, req.path);

                if (handler != null) {
                    return handler.handle(req);
                }

                // 🔥 FALLBACK → static file serving
                return serveStatic(req.path);

            } catch (Exception e) {
                return error(500, "Internal Server Error", e.getMessage());
            }
        }

        private HttpResponse serveStatic(String path) throws IOException {

            if (path.equals("/")) path = "/index.html";

            Path file = Paths.get(BASE, path.substring(1));
            System.out.println("Serving: " + file.toAbsolutePath() + " | exists: " + Files.exists(file));
            HttpResponse res = new HttpResponse();

            if (!Files.exists(file) || Files.isDirectory(file)) {
                return error(404, "Not Found", "File not found");
            }

            res.body = Files.readAllBytes(file);
            res.headers.put("Content-Type", getContentType(path));

            return res;
        }

        private HttpResponse error(int code, String status, String msg) {
            HttpResponse res = new HttpResponse();
            res.status = code;
            res.statusText = status;
            res.headers.put("Content-Type", "text/html");
            res.body = ("<h1>" + code + "</h1><p>" + msg + "</p>").getBytes();
            return res;
        }
    }

    // ===================== CONTROLLERS =====================

    static class FileController {

        public HttpResponse getIndex(HttpRequest req) throws IOException {
            Path file = Paths.get(BASE, "Public/index.html");

            HttpResponse res = new HttpResponse();

            if (!Files.exists(file)) {
                res.status = 404;
                res.body = "Index not found".getBytes();
                return res;
            }

            res.body = Files.readAllBytes(file);
            res.headers.put("Content-Type", "text/html");

            return res;
        }
    }

    static class UploadController {

        public HttpResponse upload(HttpRequest req) {
            HttpResponse res = new HttpResponse();
            res.body = "Upload endpoint hit".getBytes();
            res.headers.put("Content-Type", "text/plain");
            return res;
        }
    }

    // ===================== HELPERS =====================

    private static int findHeaderEnd(byte[] data, int start) {
        for (int i = start; i <= data.length - 4; i++)
            if (data[i]=='\r' && data[i+1]=='\n' && data[i+2]=='\r' && data[i+3]=='\n')
                return i;
        return -1;
    }

    private static String extractHeader(String headers, String name) {
        for (String line : headers.split("\r\n"))
            if (line.toLowerCase().startsWith(name.toLowerCase() + ":"))
                return line.substring(line.indexOf(':')+1).trim();
        return null;
    }

    private static Map<String, String> parseHeaders(String headers) {
        Map<String, String> map = new HashMap<>();
        for (String line : headers.split("\r\n")) {
            int idx = line.indexOf(':');
            if (idx > 0) {
                map.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
            }
        }
        return map;
    }

    private static String getContentType(String path) {
        String p = path.toLowerCase();
        if (p.endsWith(".html")) return "text/html";
        if (p.endsWith(".css")) return "text/css";
        if (p.endsWith(".js")) return "application/javascript";
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".jpg") || p.endsWith(".jpeg")) return "image/jpeg";
        if (p.endsWith(".mp4")) return "video/mp4";
        return "application/octet-stream";
    }

    private void cancelAndClose(SelectionKey key, SocketChannel client) {
        try { key.cancel(); } catch (Exception ignored) {}
        try { client.close(); } catch (Exception ignored) {}
    }

    // ===================== MAIN =====================

    public static void main(String[] args) {
        SimpleHttpServer server = new SimpleHttpServer();
        server.logger.info("Starting server on port " + PORT);
        new ServerNIO(PORT, server::handleClient).start();
    }
}