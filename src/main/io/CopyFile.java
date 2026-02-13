import java.io.*;

/**
 * Compares file copy performance using different I/O techniques.
 */
public class CopyFile {

    /** Helper method to copy from InputStream to OutputStream using a buffer
     *
     * @param in - is the input stream
     * @param out - is the output stream
     * @param bufferSize - The size of the buffer
     * @throws IOException when error occurs in input or output
     */
    private static void copyStream(InputStream in, OutputStream out, int bufferSize) throws IOException {
        byte[] buffer = new byte[bufferSize];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
    }

    /**
     * Copies the content from source file to destination file without using buffer
     * @param source - the source file
     * @param destination - the destination file where the content from source is copied
     * @return the time taken to copy the data from source to the destination in ns
     * @throws IOException when any input output error occurs
     */
    public static long copyWithoutBuffer(String source, String destination) throws IOException {
        long startTime = System.nanoTime();

        try (FileInputStream in = new FileInputStream(source);
             FileOutputStream out = new FileOutputStream(destination)) {

            int byteRead;
            while ((byteRead = in.read()) != -1) {
                out.write(byteRead);
            }
        }

        return System.nanoTime() - startTime;
    }

    /**
     * Copies file from source to destination using bufferedStreams
     * @param source - the source file
     * @param destination - the destination file where the content from the source is copied
     * @return the time taken to copy from source to destination
     * @throws IOException when any input output errors occurs
     */
    public static long copyWithBuffer(String source, String destination) throws IOException {
        long startTime = System.nanoTime();

        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(source));
             BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(destination))) {

            copyStream(in, out, 8192);
        }

        return System.nanoTime() - startTime;
    }

    /**
     * Copies the content from source file to destination file using buffer of different sizes
     * @param source - the source file
     * @param destination - the destination file where the content from the source is copied
     * @param bufferSize - the custom size of the buffer
     * @return the time taken to copy the content from source to destination
     * @throws IOException when any input output error occurs
     */
    public static long copyWithCustomBuffer(String source, String destination, int bufferSize) throws IOException {
        long startTime = System.nanoTime();

        try (FileInputStream in = new FileInputStream(source);
             FileOutputStream out = new FileOutputStream(destination)) {

            copyStream(in, out, bufferSize);
        }

        return System.nanoTime() - startTime;
    }

    /**
     * The main function
     * @param args - comment line argument
     */
    public static void main(String[] args) {
        String source = "/home/sudheshna/IdeaProjects/JavaAssignments/src/main/io/100mb.txt";
        File sourceFile = new File(source);

        if (sourceFile.exists()) {
            System.out.println("Size of file: " + sourceFile.length() + " bytes");
        } else {
            System.out.println("File does not exist!");
            return;
        }

        try {
            System.out.println("Without buffering: " + copyWithoutBuffer(source, "/home/sudheshna/IdeaProjects/JavaAssignments/src/main/ioOutput/copyWithoutBuffer.txt") + " ns");

            System.out.println("Buffered streams (default buffer): " + copyWithBuffer(source, "/home/sudheshna/IdeaProjects/JavaAssignments/src/main/ioOutput/bufferedCopy.txt") + " ns");

            int[] bufferSizes = {1024, 4096, 8192, 16384};

            for (int size : bufferSizes) {
                System.out.println("Custom buffer (" + size + " bytes): " + copyWithCustomBuffer(source, "/home/sudheshna/IdeaProjects/JavaAssignments/src/main/ioOutput/copy_" + size + ".txt", size) + " ns");
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}