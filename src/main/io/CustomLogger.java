import java.io.FileWriter;
import java.time.LocalDateTime;

/**
 * Abstract Logger class.
 * Any class extending Logger must implement the log(String msg) method.
 */
abstract class Logger {

    /**
     * Logs the given message.
     * @param msg the message to be logged
     */
    abstract void log(String msg);
}

/**
 * FileLogger logs messages into a file.
 *
 * <p>
 * This class extends the Logger abstract class and provides
 * an implementation of the log method that writes messages into a file named {@code log.txt}.
 * </p>
 */
class FileLogger extends Logger {

    /**
     * Logs the given message into the log file.
     *
     * @param msg the message to be logged
     */
    void log(String msg) {
        try {
            FileWriter fw = new FileWriter(
                    "/home/sudheshna/IdeaProjects/JavaAssignments/src/main/io/log.txt",
                    true
            );
            fw.write(msg + "\n");
            fw.close();
        } catch (Exception e) {
            System.out.println("Exception " + e + " is caught");
        }
    }
}

/**
 * ConsoleLogger logs messages to the console.
 */
class ConsoleLogger extends Logger {

    /**
     * Prints the given message to the console.
     *
     * @param msg the message to be logged
     */
    void log(String msg) {
        System.out.println(msg);
    }
}

/**
 * TimeFileLogger logs messages into a file along with timestamp.
 */
class TimeFileLogger extends FileLogger {

    /**
     * Logs the given message with a timestamp into the log file.
     *
     * @param msg the message to be logged
     */
    void log(String msg) {
        String timeMsg = LocalDateTime.now() + " : " + msg;
        super.log(timeMsg);
    }
}

/**
 * CustomLogger is the driver class.
 */
public class CustomLogger {

    public static void main(String[] args) {

        Logger l;

        l = new FileLogger();
        l.log("File log is executed");

        l = new TimeFileLogger();
        l.log("Hello");

        l = new ConsoleLogger();
        l.log("Console log is executed");
    }
}
