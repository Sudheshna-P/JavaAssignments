import java.io.*;
import java.util.List;

import logger.Logger;
import logger.LoggerManager;
import logger.LoggerFactory;
/**
 * FileCopyWO copies the contents of one file to another
 * using FileInputStream and FileOutputStream without buffering.
 * This program also prints the time taken to copy the file in milliseconds.
 */
class FileCopyWO {

    /**
     * Main method that performs file copy operation.
     *
     * @param args command-line arguments
     * @throws IOException if an I/O error occurs while reading or writing
     */
    public static void main(String[] args) throws IOException {

        File inFile = new File("src/main/io/file1.txt");
        File outFile = new File("src/main/io/file3.txt");

        Logger fileLogger = LoggerFactory.getFileLogger("filecopy.log");
        LoggerManager logger = new LoggerManager(List.of(fileLogger));

        FileInputStream in=null;
        FileOutputStream out=null;

        try {
            in = new FileInputStream(inFile);
            out = new FileOutputStream(outFile);
        }
        catch(FileNotFoundException e){
            String message = e.getMessage();
            logger.error(message);
        }

        long start = System.currentTimeMillis();
        int c;
        if (in != null) {
            while ((c = in.read()) != -1) {
                if (out != null) {
                    out.write(c);
                }
            }
        }
        long end = System.nanoTime();
        logger.info("Time taken: " + (end - start));

        if (in != null) in.close();
        if (out !=null) out.close();
    }
}