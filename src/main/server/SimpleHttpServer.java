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
    private static final String BASE    = "/home/sudheshna/IdeaProjects/JavaAssignments/src";
    private static final String UPLOADS = BASE + "/uploads"; // all uploads go here

    private static final long MAX_UPLOAD_SIZE = 500L * 1024 * 1024; // 500 MB limit

    private static final DateTimeFormatter HTTP_DATE =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
                    .withZone(ZoneOffset.UTC);

    private final LoggerManager logger;
    private final ExecutorService ioPool = Executors.newFixedThreadPool(8);

    public SimpleHttpServer() {
        Logger fileLogger = LoggerFactory.getFileLogger(
                BASE + "/main/ioOutput/server.log");
        Logger consoleLogger = LoggerFactory.getConsoleLogger();
        this.logger = new LoggerManager(List.of(fileLogger, consoleLogger));

        // Make sure uploads folder exists
        try {
            Files.createDirectories(Paths.get(UPLOADS));
        } catch (IOException e) {
            throw new RuntimeException("Cannot create uploads directory", e);
        }
    }

    // ── Main client handler ───────────────────────────────────────────────────

    private void handleClient(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();

        ByteBuffer buffer = ByteBuffer.allocate(16384);
        int bytesRead;
        try {
            bytesRead = client.read(buffer);
        } catch (IOException e) {
            key.cancel();
            client.close();
            return;
        }

        if (bytesRead == -1) {
            key.cancel();
            client.close();
            return;
        }

        buffer.flip();

        ByteArrayOutputStream acc = (ByteArrayOutputStream) key.attachment();
        if (acc == null) acc = new ByteArrayOutputStream();
        acc.write(buffer.array(), 0, buffer.limit());
        key.attach(acc);

        byte[] data = acc.toByteArray();
        int processed = 0;

        while (true) {
            int headerEnd = findHeaderEnd(data, processed);
            if (headerEnd == -1) break; // headers not fully received yet

            int end = headerEnd + 4;
            String headers = new String(data, processed, end - processed, StandardCharsets.US_ASCII);
            String requestLine = headers.lines().findFirst().orElse("");

            if (requestLine.isEmpty()) { key.cancel(); client.close(); return; }

            String[] parts = requestLine.split(" ");
            if (parts.length < 2) { key.cancel(); client.close(); return; }

            String method = parts[0];
            String path   = parts[1];
            int qIdx = path.indexOf('?');
            if (qIdx != -1) path = path.substring(0, qIdx);

            logger.info("Request: " + requestLine);

            String connectionHeader = extractHeader(headers, "Connection");
            boolean keepAlive = !"close".equalsIgnoreCase(connectionHeader);

            if (method.equals("POST")) {
                int result = handlePost(client, key, data, headers, end, keepAlive, processed);
                if (result == -1) return;
                processed = result;
                continue;
            }

            if (!method.equals("GET")) {
                sendAndMaybeClose(client, key, send405().getBytes(), keepAlive);
                processed = end;
                continue;
            }

            processed = handleGet(client, key, path, headers, keepAlive, end);
            if (processed == -1) return;
        }

        ByteArrayOutputStream newAcc = new ByteArrayOutputStream();
        if (processed < data.length)
            newAcc.write(data, processed, data.length - processed);
        key.attach(newAcc);

        if (key.isValid()) key.interestOps(SelectionKey.OP_READ);
    }

    // ── POST dispatcher ───────────────────────────────────────────────────────

    private int handlePost(SocketChannel client, SelectionKey key, byte[] data,
                           String headers, int end, boolean keepAlive, int processed) throws IOException {

        String contentType   = extractHeader(headers, "Content-Type");
        String contentLength = extractHeader(headers, "Content-Length");

        if (contentLength == null) {
            sendAndMaybeClose(client, key,
                    sendError(411, "411 Length Required", "Content-Length header missing").getBytes(),
                    keepAlive);
            return end;
        }

        long bodyLength = Long.parseLong(contentLength.trim());

        if (bodyLength > MAX_UPLOAD_SIZE) {
            sendAndMaybeClose(client, key,
                    sendError(413, "413 Payload Too Large",
                            "Max upload size is " + (MAX_UPLOAD_SIZE / 1024 / 1024) + "MB").getBytes(),
                    keepAlive);
            return end;
        }

        // Is it a multipart upload?
        if (contentType != null && contentType.toLowerCase().contains("multipart/form-data")) {
            String boundary = extractBoundary(contentType);
            if (boundary == null) {
                sendAndMaybeClose(client, key,
                        sendError(400, "400 Bad Request", "Missing boundary in Content-Type").getBytes(),
                        keepAlive);
                return end;
            }

            // Wait until the full body has arrived in the accumulator
            // For large files this is done in chunks via the accumulator
            if (data.length < end + bodyLength) {
                return processed; // not fully received yet — keep accumulating
            }

            // We have the full body — offload parsing + saving to thread pool
            final byte[] body    = Arrays.copyOfRange(data, end, (int)(end + bodyLength));
            final String bound   = boundary;
            final boolean ka     = keepAlive;

            key.interestOps(0);
            ioPool.submit(() -> {
                try {
                    handleMultipartUpload(client, key, body, bound, ka);
                } catch (Exception e) {
                    logger.info("Upload error: " + e.getMessage());
                    try { client.close(); } catch (IOException ignored) {}
                } finally {
                    if (key.isValid()) {
                        key.interestOps(SelectionKey.OP_READ);
                        key.selector().wakeup();
                    }
                }
            });

            return (int)(end + bodyLength);
        }

        // Plain (non-multipart) POST — original behaviour
        if (data.length < end + bodyLength) return processed;

        String body = new String(data, end, (int) bodyLength, StandardCharsets.UTF_8);
        logger.info("POST Body: " + body);

        String responseBody = "<h1>POST Received</h1>";
        String response = "HTTP/1.1 200 OK\r\n" +
                "Date: " + HTTP_DATE.format(ZonedDateTime.now()) + "\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + responseBody.length() + "\r\n" +
                "Connection: " + (keepAlive ? "keep-alive" : "close") + "\r\n\r\n" +
                responseBody;

        client.write(ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8)));
        if (!keepAlive) { key.cancel(); client.close(); return -1; }
        return (int)(end + bodyLength);
    }

    // ── Multipart parser + file saver ─────────────────────────────────────────

    /**
     * Parses a multipart/form-data body and saves each file part to UPLOADS/.
     *
     * Multipart body structure (all boundaries are prefixed with "--"):
     *
     *   --boundary\r\n
     *   Content-Disposition: form-data; name="file"; filename="photo.jpg"\r\n
     *   Content-Type: image/jpeg\r\n
     *   \r\n
     *   <raw file bytes>
     *   --boundary\r\n       ← next part  (or)
     *   --boundary--\r\n     ← final boundary
     */
    private void handleMultipartUpload(SocketChannel client, SelectionKey key,
                                       byte[] body, String boundary,
                                       boolean keepAlive) throws IOException {

        byte[] delimiter     = ("\r\n--" + boundary).getBytes(StandardCharsets.US_ASCII);
        byte[] firstBoundary = ("--" + boundary).getBytes(StandardCharsets.US_ASCII);

        List<String> savedFiles = new ArrayList<>();
        List<String> errors     = new ArrayList<>();

        // Find where the first part begins (skip opening "--boundary\r\n")
        int pos = indexOf(body, firstBoundary, 0);
        if (pos == -1) {
            sendAndMaybeClose(client, key,
                    sendError(400, "400 Bad Request", "Malformed multipart body").getBytes(),
                    keepAlive);
            return;
        }

        pos += firstBoundary.length; // move past "--boundary"

        // Each iteration handles one part
        while (pos < body.length) {

            // After boundary we expect either "\r\n" (more parts) or "--" (end)
            if (pos + 1 < body.length &&
                    body[pos] == '-' && body[pos + 1] == '-') {
                break; // final boundary "--boundary--", we're done
            }

            // Skip the \r\n after the boundary line
            if (pos + 1 < body.length &&
                    body[pos] == '\r' && body[pos + 1] == '\n') {
                pos += 2;
            } else {
                break; // unexpected format
            }

            // Find end of this part's headers (\r\n\r\n)
            int partHeaderEnd = indexOf(body, new byte[]{'\r','\n','\r','\n'}, pos);
            if (partHeaderEnd == -1) break;

            String partHeaders = new String(body, pos, partHeaderEnd - pos, StandardCharsets.US_ASCII);
            int dataStart = partHeaderEnd + 4; // skip \r\n\r\n

            // Find where this part's data ends (next delimiter)
            int dataEnd = indexOf(body, delimiter, dataStart);
            if (dataEnd == -1) dataEnd = body.length; // last part with no trailing \r\n

            // Extract filename from Content-Disposition
            String disposition = extractPartHeader(partHeaders, "Content-Disposition");
            String filename     = extractFilename(disposition);

            if (filename == null || filename.isEmpty()) {
                // Not a file field (e.g. a plain text field) — skip
                pos = dataEnd + delimiter.length;
                continue;
            }

            // Sanitise filename — strip any path components from client
            filename = Paths.get(filename).getFileName().toString();
            filename = filename.replaceAll("[^a-zA-Z0-9._\\-]", "_");

            // Avoid overwriting — prefix with timestamp if file exists
            Path savePath = Paths.get(UPLOADS, filename);
            if (Files.exists(savePath)) {
                String ts   = String.valueOf(System.currentTimeMillis());
                String ext  = filename.contains(".")
                        ? filename.substring(filename.lastIndexOf('.')) : "";
                String base = filename.contains(".")
                        ? filename.substring(0, filename.lastIndexOf('.')) : filename;
                filename  = base + "_" + ts + ext;
                savePath  = Paths.get(UPLOADS, filename);
            }

            long fileBytes = dataEnd - dataStart;
            logger.info("Saving upload: " + filename + " (" + fileBytes + " bytes)");

            // Write file bytes directly from body array to disk
            try {
                Files.write(savePath, Arrays.copyOfRange(body, dataStart, dataEnd),
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                savedFiles.add(filename);
                logger.info("Saved: " + savePath);
            } catch (IOException e) {
                logger.info("Failed to save " + filename + ": " + e.getMessage());
                errors.add(filename + " (write error: " + e.getMessage() + ")");
            }

            // Advance past the delimiter to the next part
            pos = dataEnd + delimiter.length;
        }

        // Build JSON-style response
        sendUploadResponse(client, key, savedFiles, errors, keepAlive);
    }

    private void sendUploadResponse(SocketChannel client, SelectionKey key,
                                    List<String> saved, List<String> errors,
                                    boolean keepAlive) {
        StringBuilder body = new StringBuilder();
        body.append("{\n");
        body.append("  \"uploaded\": [");
        for (int i = 0; i < saved.size(); i++) {
            body.append("\"").append(saved.get(i)).append("\"");
            if (i < saved.size() - 1) body.append(", ");
        }
        body.append("],\n");
        body.append("  \"errors\": [");
        for (int i = 0; i < errors.size(); i++) {
            body.append("\"").append(errors.get(i)).append("\"");
            if (i < errors.size() - 1) body.append(", ");
        }
        body.append("]\n}");

        String bodyStr = body.toString();
        int status = errors.isEmpty() ? 200 : (saved.isEmpty() ? 400 : 207);
        String statusText = status == 200 ? "OK" : status == 207 ? "Multi-Status" : "Bad Request";

        String response = "HTTP/1.1 " + status + " " + statusText + "\r\n" +
                "Date: " + HTTP_DATE.format(ZonedDateTime.now()) + "\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: " + bodyStr.length() + "\r\n" +
                "Connection: " + (keepAlive ? "keep-alive" : "close") + "\r\n\r\n" +
                bodyStr;

        try {
            client.write(ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8)));
            if (!keepAlive) { key.cancel(); client.close(); }
        } catch (IOException e) {
            logger.info("Response write error: " + e.getMessage());
        }
    }

    // ── GET handler (unchanged from before) ───────────────────────────────────

    private int handleGet(SocketChannel client, SelectionKey key, String path,
                          String headers, boolean keepAlive, int end) throws IOException {

        if (path.equals("/")) path = "/index.html";

        Path filePath = resolvePath(path);
        if (filePath == null) {
            sendAndMaybeClose(client, key, send404().getBytes(), keepAlive);
            return end;
        }
        if (!isSafePath(filePath)) {
            sendBytes(client, send403().getBytes());
            key.cancel(); client.close(); return -1;
        }
        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            sendAndMaybeClose(client, key, send404().getBytes(), keepAlive);
            return end;
        }
        if (handleCaching(client, key, headers, filePath, keepAlive)) return end;

        String rangeHeader = extractHeader(headers, "Range");
        final String finalPath = path;

        key.interestOps(0);
        if (rangeHeader != null && rangeHeader.trim().toLowerCase().startsWith("bytes=")) {
            ioPool.submit(() -> {
                try { handleRange(client, key, headers, filePath, keepAlive, finalPath); }
                catch (IOException e) {
                    logger.info("Range error: " + e.getMessage());
                    try { client.close(); } catch (IOException ignored) {}
                } finally {
                    if (key.isValid()) { key.interestOps(SelectionKey.OP_READ); key.selector().wakeup(); }
                }
            });
        } else {
            ioPool.submit(() -> {
                try { serveFullFile(client, filePath, keepAlive, finalPath); }
                catch (IOException e) {
                    logger.info("Serve error: " + e.getMessage());
                    try { client.close(); } catch (IOException ignored) {}
                } finally {
                    if (key.isValid()) { key.interestOps(SelectionKey.OP_READ); key.selector().wakeup(); }
                }
            });
        }
        return end;
    }

    // ── File serving (unchanged) ──────────────────────────────────────────────

    private void handleRange(SocketChannel client, SelectionKey key,
                             String headers, Path filePath,
                             boolean keepAlive, String path) throws IOException {

        String rangeHeader = extractHeader(headers, "Range");
        long fileSize = Files.size(filePath);
        long start = 0, endByte = fileSize - 1;

        try {
            String[] parts = rangeHeader.substring(6).trim().split("-", 2);
            if (!parts[0].isEmpty()) start   = Long.parseLong(parts[0].trim());
            if (parts.length > 1 && !parts[1].isEmpty()) endByte = Long.parseLong(parts[1].trim());
        } catch (Exception ignored) {}

        if (start > endByte || start >= fileSize) {
            String resp = "HTTP/1.1 416 Range Not Satisfiable\r\n" +
                    "Content-Range: bytes */" + fileSize + "\r\n" +
                    "Content-Length: 0\r\n\r\n";
            sendAndMaybeClose(client, key, resp.getBytes(), keepAlive);
            return;
        }

        endByte = Math.min(endByte, fileSize - 1);
        long contentLength = endByte - start + 1;
        String lastModified = HTTP_DATE.format(Files.getLastModifiedTime(filePath).toInstant());

        String header = "HTTP/1.1 206 Partial Content\r\n" +
                "Date: " + HTTP_DATE.format(ZonedDateTime.now()) + "\r\n" +
                "Last-Modified: " + lastModified + "\r\n" +
                "Content-Type: " + getContentType(path) + "\r\n" +
                "Content-Length: " + contentLength + "\r\n" +
                "Content-Range: bytes " + start + "-" + endByte + "/" + fileSize + "\r\n" +
                "Accept-Ranges: bytes\r\n" +
                "Connection: " + (keepAlive ? "keep-alive" : "close") + "\r\n\r\n";

        try {
            client.write(ByteBuffer.wrap(header.getBytes(StandardCharsets.US_ASCII)));
            try (FileChannel fc = FileChannel.open(filePath, StandardOpenOption.READ)) {
                long pos = start, remaining = contentLength;
                while (remaining > 0) {
                    long sent = fc.transferTo(pos, remaining, client);
                    if (sent <= 0) break;
                    pos += sent; remaining -= sent;
                }
            }
        } catch (IOException e) {
            logger.info("Client disconnected during range: " + e.getMessage());
            client.close(); return;
        }
        logger.info("206 Served: " + path + " bytes=" + start + "-" + endByte);
        if (!keepAlive) client.close();
    }

    private void serveFullFile(SocketChannel client, Path filePath,
                               boolean keepAlive, String path) throws IOException {
        long fileSize = Files.size(filePath);
        String lastModified = HTTP_DATE.format(Files.getLastModifiedTime(filePath).toInstant());

        String header = "HTTP/1.1 200 OK\r\n" +
                "Date: " + HTTP_DATE.format(ZonedDateTime.now()) + "\r\n" +
                "Last-Modified: " + lastModified + "\r\n" +
                "Content-Type: " + getContentType(path) + "\r\n" +
                "Content-Length: " + fileSize + "\r\n" +
                "Accept-Ranges: bytes\r\n" +
                "Connection: " + (keepAlive ? "keep-alive" : "close") + "\r\n\r\n";

        try {
            client.write(ByteBuffer.wrap(header.getBytes(StandardCharsets.US_ASCII)));
            try (FileChannel fc = FileChannel.open(filePath, StandardOpenOption.READ)) {
                long pos = 0, remaining = fileSize;
                while (remaining > 0) {
                    long sent = fc.transferTo(pos, remaining, client);
                    if (sent <= 0) break;
                    pos += sent; remaining -= sent;
                }
            }
        } catch (IOException e) {
            logger.info("Client disconnected during full file: " + e.getMessage());
            client.close(); return;
        }
        logger.info("200 Served: " + path);
        if (!keepAlive) client.close();
    }

    private Path resolvePath(String path) {
        String folder = "Public", prefix = "";
        if (path.startsWith("/images/"))      { folder = "images";    prefix = "/images"; }
        else if (path.startsWith("/files/"))  { folder = "documents"; prefix = "/files"; }
        else if (path.startsWith("/video/"))  { folder = "video";     prefix = "/video"; }
        else if (path.startsWith("/uploads/")){ folder = "uploads";   prefix = "/uploads"; }

        String relative = prefix.isEmpty() ? path : path.substring(prefix.length());
        if (relative.isEmpty() || relative.equals("/")) return null;
        String fileRelative = relative.startsWith("/") ? relative.substring(1) : relative;
        if (fileRelative.isEmpty()) return null;
        return Paths.get(BASE, folder, fileRelative);
    }

    private boolean isSafePath(Path filePath) {
        return filePath.normalize().toAbsolutePath()
                .startsWith(Paths.get(BASE).toAbsolutePath().normalize());
    }

    private boolean handleCaching(SocketChannel client, SelectionKey key,
                                  String headers, Path filePath, boolean keepAlive) {
        String ifModSince = extractHeader(headers, "If-Modified-Since");
        if (ifModSince == null) return false;
        try {
            ZonedDateTime clientTime = ZonedDateTime.parse(ifModSince.trim(), HTTP_DATE);
            long lastMod  = Files.getLastModifiedTime(filePath).toMillis() / 1000 * 1000;
            long clientMs = clientTime.toInstant().toEpochMilli() / 1000 * 1000;
            if (lastMod <= clientMs) {
                String response = "HTTP/1.1 304 Not Modified\r\n" +
                        "Date: " + HTTP_DATE.format(ZonedDateTime.now()) + "\r\n" +
                        "Last-Modified: " +
                        HTTP_DATE.format(Files.getLastModifiedTime(filePath).toInstant()) + "\r\n" +
                        "Connection: " + (keepAlive ? "keep-alive" : "close") + "\r\n\r\n";
                sendAndMaybeClose(client, key, response.getBytes(StandardCharsets.US_ASCII), keepAlive);
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    // ── Multipart parsing helpers ─────────────────────────────────────────────

    /**
     * Extracts boundary from:  Content-Type: multipart/form-data; boundary=----WebKitFormBoundary...
     */
    private static String extractBoundary(String contentType) {
        for (String part : contentType.split(";")) {
            part = part.trim();
            if (part.toLowerCase().startsWith("boundary=")) {
                return part.substring("boundary=".length()).trim();
            }
        }
        return null;
    }

    /**
     * Extracts filename from:
     * Content-Disposition: form-data; name="file"; filename="photo.jpg"
     */
    private static String extractFilename(String disposition) {
        if (disposition == null) return null;
        for (String part : disposition.split(";")) {
            part = part.trim();
            if (part.toLowerCase().startsWith("filename=")) {
                String val = part.substring("filename=".length()).trim();
                // Strip surrounding quotes if present
                if (val.startsWith("\"") && val.endsWith("\""))
                    val = val.substring(1, val.length() - 1);
                return val;
            }
        }
        return null;
    }

    /**
     * Finds the first occurrence of `pattern` in `data` starting from `fromIndex`.
     * Standard byte-array search (like String.indexOf but for byte[]).
     */
    private static int indexOf(byte[] data, byte[] pattern, int fromIndex) {
        outer:
        for (int i = fromIndex; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    private static String extractPartHeader(String partHeaders, String name) {
        for (String line : partHeaders.split("\r\n")) {
            if (line.toLowerCase().startsWith(name.toLowerCase() + ":"))
                return line.substring(line.indexOf(':') + 1).trim();
        }
        return null;
    }

    // ── Response helpers ──────────────────────────────────────────────────────

    private void sendAndMaybeClose(SocketChannel client, SelectionKey key,
                                   byte[] data, boolean keepAlive) {
        try {
            client.write(ByteBuffer.wrap(data));
            if (!keepAlive) { key.cancel(); client.close(); }
        } catch (IOException e) {
            logger.info("Send error: " + e.getMessage());
            try { key.cancel(); client.close(); } catch (IOException ignored) {}
        }
    }

    private void sendBytes(SocketChannel client, byte[] data) {
        try { client.write(ByteBuffer.wrap(data)); }
        catch (IOException e) { logger.info("Send error: " + e.getMessage()); }
    }

    private static String sendError(int code, String status, String message) {
        String body = "<h1>" + status + "</h1><p>" + message + "</p>";
        return "HTTP/1.1 " + code + " " + status + "\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + body.length() + "\r\n\r\n" + body;
    }

    private static String send404() { return sendError(404, "404 Not Found", "Resource not found"); }
    private static String send405() { return sendError(405, "405 Method Not Allowed", "Method not allowed"); }
    private static String send403() { return sendError(403, "403 Forbidden", "Access denied"); }

    // ── Misc helpers ──────────────────────────────────────────────────────────

    private static int findHeaderEnd(byte[] data, int start) {
        for (int i = start; i <= data.length - 4; i++) {
            if (data[i]=='\r' && data[i+1]=='\n' && data[i+2]=='\r' && data[i+3]=='\n')
                return i;
        }
        return -1;
    }

    private static String extractHeader(String headers, String name) {
        for (String line : headers.split("\r\n")) {
            if (line.toLowerCase(Locale.ROOT).startsWith(name.toLowerCase(Locale.ROOT) + ":"))
                return line.substring(line.indexOf(':') + 1).trim();
        }
        return null;
    }

    private static String getContentType(String path) {
        String p = path.toLowerCase(Locale.ROOT);
        if (p.endsWith(".html") || p.endsWith(".htm")) return "text/html; charset=utf-8";
        if (p.endsWith(".css"))  return "text/css";
        if (p.endsWith(".js"))   return "application/javascript";
        if (p.endsWith(".png"))  return "image/png";
        if (p.endsWith(".jpg") || p.endsWith(".jpeg")) return "image/jpeg";
        if (p.endsWith(".gif"))  return "image/gif";
        if (p.endsWith(".mp4"))  return "video/mp4";
        if (p.endsWith(".webm")) return "video/webm";
        if (p.endsWith(".ogg"))  return "video/ogg";
        if (p.endsWith(".pdf"))  return "application/pdf";
        if (p.endsWith(".json")) return "application/json";
        return "application/octet-stream";
    }

    public static void main(String[] args) {
        SimpleHttpServer server = new SimpleHttpServer();
        server.logger.info("Starting server on port " + PORT);
        new ServerNIO(PORT, server::handleClient).start();
    }
}