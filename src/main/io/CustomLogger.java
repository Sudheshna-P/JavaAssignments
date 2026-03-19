import java.io.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.time.format.DateTimeFormatter;

import static java.lang.System.exit;

enum LogLevel {
    INFO,
    DEBUG,
    ERROR
}

abstract class Logger {
    abstract void log(LogLevel level, String message);
}


class FileLogger extends Logger {

    private final String filePath;
    private final long maxFileSize;

    private BufferedWriter bw;
    private final BlockingQueue<String> logQueue = new LinkedBlockingQueue<>();

    /**
     * Logs the messages in File
     * @param filePath - path of file
     * @param maxFileSize - maximum size of file
     */
    public FileLogger(String filePath, long maxFileSize) {
        this.filePath = filePath;
        this.maxFileSize = maxFileSize;

        try {
            this.bw = new BufferedWriter(new FileWriter(filePath, true));
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize FileLogger", e);
        }

        startWorker();
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {flushLogs();}));
//        try {
//            bw.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    /**
     *
     * @param level
     * @param message
     */
    @Override
    void log(LogLevel level, String message) {
        String logMessage = LocalDateTime.now() + " [" + level + "] " + message;
        if (!logQueue.offer(logMessage)) {
            System.err.println("Log queue is full");
        }
    }

    private void startWorker() {
        Thread worker = new Thread(() -> {

            while (true) {
                try {
                    String logMessage = logQueue.take();

                    rotateFileIfNeeded();

                    bw.write(logMessage);
                    bw.newLine();
                    bw.flush();

                } catch (IOException | InterruptedException e) {
                    System.err.println("Error writing log: " + e.getMessage());
                }
            }

        });

        worker.setDaemon(true);
        worker.start();
    }

    private void rotateFileIfNeeded() throws IOException {

        File file = new File(filePath);

        if (!file.exists() || file.length() < maxFileSize) return;

        bw.close();

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

        String rotatedName = filePath.replace(".log", "") + "-" + timestamp + ".log";

        File rotatedFile = new File(rotatedName);

        if (!file.renameTo(rotatedFile)) {
            throw new IOException("Failed to rotate log file");
        }

        deleteOldLogs();

        bw = new BufferedWriter(new FileWriter(filePath, true));
    }

    private void deleteOldLogs() {

        File dir = new File(".");
        String baseName = filePath.replace(".log", "");

        File[] files = dir.listFiles((d, name) ->
                name.startsWith(baseName + "-") && name.endsWith(".log")
        );

        int maxBackupFiles = 5;

        if (files == null || files.length <= maxBackupFiles) return;

        Arrays.sort(files, Comparator.comparingLong(File::lastModified));

        for (int i = 0; i < files.length - maxBackupFiles; i++) {
            if (!files[i].delete()) {
                System.err.println("Failed to delete old log file: " + files[i].getName());
            }
        }
    }

//    public void flushLogs() {
//        try {
//            while (!logQueue.isEmpty()) {
//                String msg = logQueue.poll();
//                if (msg != null) {
//                    bw.write(msg);
//                    bw.newLine();
//                }
//            }
//            bw.flush();
//        } catch (IOException e) {
//            System.err.println("Flush failed: " + e.getMessage());
//        }
//    }
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
        return loggers.computeIfAbsent(filePath, path -> {
            try {
                return new FileLogger(path, 1024 * 1024);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create FileLogger for: " + path, e);
            }
        });
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
        //System.exit(0);
    }
}