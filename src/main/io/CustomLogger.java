import java.io.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

enum LogLevel {
    INFO,
    DEBUG,
    ERROR
}

abstract class Logger {
    abstract void log(LogLevel level, String message);
}

class FileLogger extends Logger {

    private final String baseFileName;
    private final long maxFileSize;
    private final int maxBackups;

    private BufferedWriter bw;
    private File currentFile;

    private final BlockingQueue<String> logQueue = new LinkedBlockingQueue<>();

    public FileLogger(String baseFileName, long maxFileSize, int maxBackups) {
        this.baseFileName = baseFileName;
        this.maxFileSize = maxFileSize;
        this.maxBackups = maxBackups;

        createNewLogFile();
        logWriter();
    }

    @Override
    public void log(LogLevel level, String message) {
        String logMessage = LocalDateTime.now() + " [" + level + "] " + message;
        if (!logQueue.offer(logMessage)) {
            System.err.println("Log queue is full");
        }
    }

    private void logWriter() {
        Thread worker = new Thread(() -> {
            while (true) {
                try {
                    String logMessage = logQueue.take();

                    rotateIfNeeded();

                    bw.write(logMessage);
                    bw.newLine();
                    bw.flush(); // ✅ FIX

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // worker.setDaemon(true);
        worker.start();
    }

    // 🔥 Hybrid rotation logic
    private void rotateIfNeeded() throws IOException {
        if (currentFile.length() < maxFileSize) return;

        bw.close();
        createNewLogFile();
        cleanupOldFiles();
    }

    private void createNewLogFile() {
        try {
            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

            String fileName = baseFileName + "-" + timestamp + ".log";

            currentFile = new File(fileName);
            bw = new BufferedWriter(new FileWriter(currentFile, true));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // ✅ Delete oldest files if limit exceeded
    private void cleanupOldFiles() {
        File dir = new File(".");
        File[] logFiles = dir.listFiles((d, name) ->
                name.startsWith(baseFileName) && name.endsWith(".log")
        );

        if (logFiles == null || logFiles.length <= maxBackups) return;

        // Sort by last modified (oldest first)
        Arrays.sort(logFiles, (f1, f2) ->
                Long.compare(f1.lastModified(), f2.lastModified())
        );

        int filesToDelete = logFiles.length - maxBackups;

        for (int i = 0; i < filesToDelete; i++) {
            if (!logFiles[i].delete()) {
                System.err.println("Failed to delete: " + logFiles[i].getName());
            }
        }
    }
}


class ConsoleLogger extends Logger {

    @Override
    void log(LogLevel level, String message) {
        String logMessage = LocalDateTime.now() + " [" + level + "] " + message;
        try {
            System.out.println(logMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class LoggerFactory {

    private static final ConcurrentHashMap<String, Logger> loggers = new ConcurrentHashMap<>();

    public static Logger getFileLogger(String filePath) {
        return loggers.computeIfAbsent(filePath,
                path -> new FileLogger(path, 1024 * 1024, 10)); // 1MB, max 10 files
    }

    public static Logger getConsoleLogger() {
        return new ConsoleLogger();
    }
}

class LoggerManager {

    private final List<Logger> loggers;

    public LoggerManager(List<Logger> loggers) {
        this.loggers = loggers;
    }

    private void log(LogLevel level, String msg) {
        for (Logger logger : loggers) {
            try {
                logger.log(level, msg);
            } catch (Exception e) {
                System.err.println("Logger failed "+ e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void info(String message) {
        log(LogLevel.INFO, message);
    }

    public void debug(String message) {
        log(LogLevel.DEBUG, message);
    }

    public void error(String message) {
        log(LogLevel.ERROR, message);
    }
}

public class CustomLogger {

    public static void main(String[] args) {

        Logger fileLogger = LoggerFactory.getFileLogger("application.log");
        Logger consoleLogger = LoggerFactory.getConsoleLogger();

        LoggerManager manager = new LoggerManager(
                Arrays.asList(fileLogger, consoleLogger)
        );

        manager.info("Application started");
        manager.debug("Debugging info");
        manager.error("Something went wrong");

        Runnable task = () -> {
            for (int i = 0; i < 5; i++) {
                manager.info(Thread.currentThread().getName() + " message " + i);
            }
        };

        Thread t1 = new Thread(task, "Thread-1");
        Thread t2 = new Thread(task, "Thread-2");

        t1.start();
        t2.start();
    }
}