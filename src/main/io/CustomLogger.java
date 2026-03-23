import java.io.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.time.format.DateTimeFormatter;


enum LogLevel {
    INFO,
    DEBUG,
    ERROR
}

/**
 * Abstract class for logger implementation
 * Subclasses must implement log(LogLevel, String) method
 */
interface Logger {
    /**
     * Logs the message at the severity level
     * @param level the severity level
     * @param message the log message to record
     */
    void log(LogLevel level, String message) throws IOException;
}

/**
 * Logs messages to the file
 */
class FileLogger implements Logger {

    private final String filePath;
    private final long maxFileSize;

    private BufferedWriter bw;
    private final BlockingQueue<String> logQueue = new LinkedBlockingQueue<>(10000);

    /**
     * Logs the messages in File
     * @param filePath - path of file
     * @param maxFileSize - maximum size of file
     * @throws RuntimeException is the file cannot be opened or created
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

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running = false;
            workerThread.interrupt();
            try {
                workerThread.join(); // wait for worker to fully stop
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            flushLogs();  // now safe, worker is completely done
            try {
                bw.close();
            } catch (IOException e) {
                System.err.println("Failed to close BufferedWriter for file " + filePath + ": " + e.getMessage());
            }
        }));
    }

    /**
     * Enqueues the log messages to be written to the file
     * @param level the severity level of the message
     * @param message the log message
     */
    @Override
    public void log(LogLevel level, String message) {
        String logMessage = LocalDateTime.now() + " [" + level + "] " + message;
        if (!logQueue.offer(logMessage)) {
            System.err.println("Log queue is full, dropping message: " + logMessage);
        }
    }

    /**
     * Starts the background worker thread that writes the entries into the file
     * the thread is marked Daemon
     */
    private volatile boolean running = true;
    private Thread workerThread; // keep reference to worker

    private void startWorker() {
        workerThread = new Thread(() -> {
            while (running || !logQueue.isEmpty()) {
                try {
                    String logMessage = logQueue.take();
                    rotateFileIfNeeded();
                    bw.write(logMessage);
                    bw.newLine();
                    bw.flush();
                } catch (IOException e) {
                    System.err.println("Worker thread failed: " + e.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // restore interrupt flag
                    break; // exit loop cleanly
                }
            }
        });
        workerThread.setDaemon(true);
        workerThread.start();
    }



    /**
     * Checks whether the current log file exceeded the maxFileSize and,
     * if so, it renames the file with timestamp and opens a new log file
     * @throws IOException if the file cannot be opened or closed
     */
    private void rotateFileIfNeeded() throws IOException {

        File file = new File(filePath);

        if (!file.exists() || file.length() < maxFileSize) return;

        bw.close();

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss.SSS"));
        String rotatedName = filePath.replace(".log", "") + "-" + timestamp + ".log";

        File rotatedFile = new File(rotatedName);

        if (!file.renameTo(rotatedFile)) {
            throw new IOException("Failed to rotate log file from " + filePath + " to " + rotatedName);
        }

        deleteOldLogs();

        bw = new BufferedWriter(new FileWriter(filePath, true));
    }

    /**
     * Deletes the oldest rotated log backup files when the number of backups
     * exceeds the maximum allowed limit.
     * the maximum number of backup files is 5
     */
    private void deleteOldLogs() {
        File dir = new File(filePath).getParentFile();
        if (dir == null) dir = new File(".");

        String baseName = new File(filePath).getName().replace(".log", "");

        File[] files = dir.listFiles((d, name) ->
                name.startsWith(baseName + "-") && name.endsWith(".log"));

        int maxBackupFiles = 5;
        if (files == null || files.length <= maxBackupFiles) return;

        Arrays.sort(files, Comparator.comparingLong(File::lastModified));

        for (int i = 0; i < files.length - maxBackupFiles; i++) {
            if (!files[i].delete()) {
                System.err.println("Failed to delete old log file: " + files[i].getAbsolutePath());
            }
        }
    }

    public void flushLogs() {
        try {
            while (!logQueue.isEmpty()) {
                String msg = logQueue.poll();
                if (msg != null) {
                    bw.write(msg);
                    bw.newLine();
                }
            }
            bw.flush();
        } catch (IOException e) {
            System.err.println("Flush failed: " + e.getMessage());
        }
    }
}

/**
 * Writes the log messages to the console
 */
class ConsoleLogger implements Logger {

    /**
     * Writes the log message to the console
     * @param level the severity level
     * @param message the log message to record
     */
    @Override
    public void log(LogLevel level, String message) {
        String logMessage = LocalDateTime.now() + " [" + level + "] " + message;
        System.out.println(logMessage);
    }
}
/**
 * Factory class for creating the Logger instance
 */
class LoggerFactory {

    private static final ConcurrentHashMap<String, Logger> loggers = new ConcurrentHashMap<>();

    /**
     * Returns a FileLogger for the give path
     * if the specified path is already created, the cached instance is returned
     * else a new FileLogger is created with default max file size of 1MB
     * @param filePath the path of the log file
     * @return a Logger that writes to the specific file
     * @throws RuntimeException if the Logger cannot be initialized
     */
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

    /**
     * Creates and returns a new Console Logger
     * @return a new Logger that writes to the console
     */
    public static Logger getConsoleLogger() {
        return new ConsoleLogger();
    }
}

/**
 * Manages the collection of logger instances
 */
class LoggerManager {

    private final List<Logger> loggers;

    /**
     * Constructs a LoggerManager with the provided list of loggers
     * @param loggers - loggers
     */
    public LoggerManager(List<Logger> loggers) {
        this.loggers = loggers;
    }

    private void log(LogLevel level, String msg) {
        for (Logger logger : loggers) {
            try {
                logger.log(level, msg);
            } catch (Exception e) {
                System.err.println("Logger failed " + e.getMessage());
            }
        }
    }
    /**
     * Logs a message in INFO level
     * @param message the info message
     */
    public void info(String message) {
        log(LogLevel.INFO, message);
    }

    /**
     * Logs a message at DEBUG level
     * @param message the debug message
     */
    public void debug(String message) {
        log(LogLevel.DEBUG, message);
    }

    /**
     * Logs a message at ERROR level
     * @param message the error message
     */
    public void error(String message) {
        log(LogLevel.ERROR, message);
    }
}

/**
 * The CustomLogger
 */
public class CustomLogger {

    /**
     * Application entry point
     * @param args command line argument
     */
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