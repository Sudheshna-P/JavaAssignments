import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

abstract class Logger {
    abstract void log(String msg);
}

class FileLogger extends Logger {
    private final String filePath;
    private final Object lock = new Object();

    public FileLogger(String filePath) {
        this.filePath = filePath;
    }

    @Override
    void log(String msg) {
        synchronized (lock) {
            try (FileWriter fw = new FileWriter(filePath, true)) {
                fw.write(msg + "\n");
            } catch (IOException e) {
                System.out.println("Exception " + e + " is caught");
            }
        }
    }
}

class TimeFileLogger extends FileLogger {
    public TimeFileLogger(String filePath) {
        super(filePath);
    }

    @Override
    void log(String msg) {
        String timeMsg = LocalDateTime.now() + " : " + msg;
        super.log(timeMsg);
    }
}

class ConsoleLogger extends Logger {
    @Override
    void log(String msg) {
        System.out.println(msg);
    }
}

class LoggerManager {
    private Logger logger;

    public LoggerManager(Logger logger) {
        this.logger = logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void log(String msg) {
        logger.log(msg);
    }
}

public class CustomLogger {
    public static void main(String[] args) {

        String logFilePath = "/home/sudheshna/IdeaProjects/JavaAssignments/src/main/ioOutput/log.txt";

        LoggerManager manager = new LoggerManager(new FileLogger(logFilePath));
        manager.log("File log executed");

        manager.setLogger(new TimeFileLogger(logFilePath));
        manager.log("Timestamped log executed");

        manager.setLogger(new ConsoleLogger());
        manager.log("Console log executed");

        Runnable task = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 5; i++) {
                    manager.log(Thread.currentThread().getName() + " logging " + i);
                }
            }
        };

        Thread t1 = new Thread(task, "Thread-1");
        Thread t2 = new Thread(task, "Thread-2");

        t1.start();
        t2.start();
    }
}