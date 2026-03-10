import java.io.*;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

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
    private final Object lock = new Object();

    public FileLogger(String filePath, long maxFileSize) {
        this.filePath = filePath;
        this.maxFileSize = maxFileSize;
    }

    @Override
    public void log(LogLevel level, String message) {

        synchronized (lock) {
            try {

                rotateFileIfNeeded();

                try (FileWriter fw = new FileWriter(filePath, true);
                     BufferedWriter bw = new BufferedWriter(fw)) {

                    String logMessage = LocalDateTime.now()
                            + " [" + level + "] "
                            + message;

                    bw.write(logMessage);
                    bw.newLine();
                }

            } catch (IOException e) {
                System.out.println("Logging error: " + e);
            }
        }
    }

    private void rotateFileIfNeeded() {

        File file = new File(filePath);

        if (file.exists() && file.length() >= maxFileSize) {

            File backup = new File(filePath + ".1");

            if (backup.exists()) {
                backup.delete();
            }

            file.renameTo(backup);
        }
    }
}

class ConsoleLogger extends Logger {

    @Override
    void log(LogLevel level, String message) {

        String logMessage = LocalDateTime.now()
                + " [" + level + "] "
                + message;

        System.out.println(logMessage);
    }
}

class LoggerFactory {

    private static final ConcurrentHashMap<String, Logger> loggers = new ConcurrentHashMap<>();

    public static Logger getFileLogger(String filePath) {

        return loggers.computeIfAbsent(filePath,
                path -> new FileLogger(path, 1024 * 1024));
    }

    public static Logger getConsoleLogger() {
        return new ConsoleLogger();
    }
}

class LoggerManager {

    private Logger logger;

    public LoggerManager(Logger logger) {
        this.logger = logger;
    }

    public synchronized void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void info(String message) {
        logger.log(LogLevel.INFO, message);
    }

    public void debug(String message) {
        logger.log(LogLevel.DEBUG, message);
    }

    public void error(String message) {
        logger.log(LogLevel.ERROR, message);
    }
}

public class CustomLogger {

    public static void main(String[] args) {

        Logger fileLogger = LoggerFactory.getFileLogger("application.log");

        LoggerManager manager = new LoggerManager(fileLogger);

        manager.info("Application started");
        manager.debug("Debugging info");
        manager.error("Something went wrong");

        Logger consoleLogger = LoggerFactory.getConsoleLogger();
        manager.setLogger(consoleLogger);

        manager.info("Logging to console now");

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