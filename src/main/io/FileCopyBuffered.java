import java.io.*;
/**
 * FileCopyBuffered copies the contents of one file to another
 * using BufferedInputStream and BufferedOutputStream.
 * This program also prints the time taken to copy the file in milliseconds.
 */
class FileCopyBuffered {

    /**
     * Main method that performs file copy operation.
     * copies the data from source to destination
     * throws ioexception if any i/o error occurs while reading or writing
     *
     * @param args command-line arguments
     * @throws IOException if an I/O error occurs while reading or writing
     */
    public static void main(String[] args) throws IOException {

        File inFile = new File("src/main/io/file1.txt");
        File outFile = new File("src/main/io/file2.txt");

        BufferedInputStream in=null;
        BufferedOutputStream out=null;

        try {
            in = new BufferedInputStream(new FileInputStream(inFile));
            out = new BufferedOutputStream(new FileOutputStream(outFile));
        }
        catch(FileNotFoundException e){
            System.out.println(e.getMessage());
        }

        long start = System.nanoTime();
        int c;
        if (in != null) {
            while ((c = in.read()) != -1) {
                if (out != null) {
                    out.write(c);
                }
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("Time taken: " + (end - start));

        if (in != null) in.close();
        if (out !=null) out.close();
    }
}