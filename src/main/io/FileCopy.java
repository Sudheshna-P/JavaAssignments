import java.io.*;

/**
 * Compares file copy performance using different I/O techniques.
 */
public class FileCopy {

    /**
     * It copies the file from source to destination without using BufferedStream
     *
     * @param source the source file
     * @param destination the destination file the source is copied
     * @return the time taken to copy from source to destination
     * @throws IOException when any input output error occurs
     */
    public static long copyWithoutBuffer(String source, String destination) throws IOException {

        long startTime = System.nanoTime();

        try (FileInputStream inputStream = new FileInputStream(source);
             FileOutputStream outputStream = new FileOutputStream(destination)) {

            int byteRead;
            while ((byteRead = inputStream.read()) != -1) {
                outputStream.write(byteRead);
            }
        }

        return System.nanoTime() - startTime;
    }


    /**
     * Copying file from source to destination using bufferedStream
     *
     * @param source it is the source file
     * @param destination it is the destination file where the data is copied
     * @return the time taken to copy from source to destination
     * @throws IOException when any input output error occurs
     */
    public static long copyWithBuffer(String source, String destination) throws IOException {

        long startTime = System.nanoTime();

        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(source));
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(destination))) {

            int byteRead;
            while ((byteRead = bufferedInputStream.read()) != -1) {
                bufferedOutputStream.write(byteRead);
            }
        }

        return System.nanoTime() - startTime;
    }

    /**
     * Copies the content from source file to destination file using buffer of different sizes
     * @param source the source file
     * @param destination the destination file where the data is copied
     * @param bufferSize the custom size of the buffer
     * @return the time taken to copy the data from source to destination
     * @throws IOException when any input output error occurs
     */
    public static long copyWithCustomBuffer(String source,String destination,int bufferSize) throws IOException {

        long startTime = System.nanoTime();

        try (FileInputStream inputStream = new FileInputStream(source);
             FileOutputStream outputStream = new FileOutputStream(destination)) {

            byte[] buffer = new byte[bufferSize];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        return System.nanoTime() - startTime;
    }

    /**
     * The main function
     * @param args comment line argument
     */
    public static void main(String[] args) {

        String source = "/home/sudheshna/IdeaProjects/JavaAssignments/src/main/io/100mb.txt";
        System.out.println("size of file"+source.length());

        try {
            System.out.println("Without buffering: " + copyWithoutBuffer(source, "/home/sudheshna/IdeaProjects/JavaAssignments/src/main/ioOutput/testOut.txt") + " ns");

            System.out.println("Buffered streams (default buffer): " + copyWithBuffer(source, "/home/sudheshna/IdeaProjects/JavaAssignments/src/main/io/bufferedCopy.txt") + " ns");

            int[] bufferSizes = {1024, 4096, 8192, 16384};

            for (int size : bufferSizes) {
                System.out.println("Custom buffer (" + size + " bytes): " + copyWithCustomBuffer(source, "/home/sudheshna/IdeaProjects/JavaAssignments/src/main/io/copy_" + size + ".txt", size) + " ns");
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
