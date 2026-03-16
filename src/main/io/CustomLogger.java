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

//@SuppressWarnings("ALL")
class FileLogger extends Logger {

    private final String filePath;
    private final long maxFileSize;
    private final Object lock = new Object();

    private final BlockingQueue<String> logQueue = new LinkedBlockingQueue<>();

    public FileLogger(String filePath, long maxFileSize) {
        this.filePath = filePath;
        this.maxFileSize = maxFileSize;

        logWriter();
    }

    @Override
    public void log(LogLevel level, String message) {

        String logMessage = LocalDateTime.now() + " [" + level + "] " + message;

        logQueue.offer(logMessage);
    }

    private void logWriter() {

        Thread worker = new Thread(() -> {

            while (true) {

                try {
                    String logMessage = logQueue.take();

                    rotateFileIfNeeded();

                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath, true))) {
                        bw.write(logMessage);
                        bw.newLine();
                    }


                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        worker.setDaemon(true);
        worker.start();
    }

    private void rotateFileIfNeeded() {

        File file = new File(filePath);

        if (file.exists() && file.length() >= maxFileSize) {

            int maxBackups = 3;

            File oldest = new File(filePath + "." + maxBackups);
            if (oldest.exists()) {
                oldest.delete();
            }

            for (int i = maxBackups - 1; i>= 1; i--) {

                File current = new File(filePath + "." + i);

                if (current.exists()) {
                    File next = new File(filePath + "." + (i + 1));
                    current.renameTo(next);

                }
            }

            File firstBackup = new File(filePath + ".1");
            file.renameTo(firstBackup);

        }
    }
}

class ConsoleLogger extends Logger {

    @Override
    void log(LogLevel level, String message) {

        String logMessage = LocalDateTime.now() + " (" + level + ") " + message;

        System.out.println(logMessage);
    }
}

class LoggerCreator {

    private static final ConcurrentHashMap<String, Logger> loggers = new ConcurrentHashMap<>();

    public static Logger getFileLogger(String filePath) {

        return loggers.computeIfAbsent(filePath, path -> new FileLogger(path, 1024 * 1024));
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

    public void info(String message) {
        for (Logger logger : loggers) {
            logger.log(LogLevel.INFO, message);
        }
    }

    public void debug(String message) {
        for (Logger logger : loggers) {
            logger.log(LogLevel.DEBUG, message);
        }
    }

    public void error(String message) {
        for (Logger logger : loggers) {
            logger.log(LogLevel.ERROR, message);
        }
    }
}

public class CustomLogger {

    public static void main(String[] args) {

        Logger fileLogger = LoggerCreator.getFileLogger("application.log");
        Logger consoleLogger = LoggerCreator.getConsoleLogger();

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