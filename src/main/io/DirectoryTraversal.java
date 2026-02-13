import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * DirectoryFileSearcher
 * Traverses a given directory and its subdirectories to:
 * 1. List all files with a specific extension.
 * 2. Search for files containing a specific keyword.
 */
public class DirectoryTraversal {

    /**
     * Recursively traverses directory and collects files
     * that match the given extension.
     */
    public static List<File> getFilesByExtension(File directory, String extension) {
        List<File> matchedFiles = new ArrayList<>();

        File[] files = directory.listFiles();
        if (files == null) return matchedFiles;

        for (File file : files) {
            if (file.isDirectory()) {
                getFilesByExtension(file, extension);
            } else if (file.isFile() && file.getName().endsWith(extension)) {
                matchedFiles.add(file);
            }
        }

        return matchedFiles;
    }

    /**
     * Prints file name and absolute path of the file.
     */
    private static void printFileDetails(File file) {
        System.out.println("File Name: " + file.getName());
        System.out.println("Absolute Path: " + file.getAbsolutePath());
        System.out.println();
    }

    /**
     * Checks whether the file contains the given keyword.
     * @param file - the file to be checked if the keyword is present
     * @param keyword - the keyword that is searched
     * @return true if the file contains the keyword else it returns false
     */
    public static boolean containsKeyword(File file, String keyword) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(keyword)) {
                    return true;
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + file.getAbsolutePath());
        }
        return false;
    }

    /**
     * Recursively searches all files in the directory
     * for the given word. Searches the keyword in the entire directory regardless of the extension
     */
    public static void searchKeywordInDirectory(File directory, String keyword) {

        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {

            if (file.isDirectory()) {
                searchKeywordInDirectory(file, keyword);
            }

            else if (file.isFile()) {
                if (containsKeyword(file, keyword)) {
                    System.out.println("Keyword found in: " + file.getAbsolutePath());
                }
            }
        }
    }

    /**
     * The main function where the program starts
     * @param args - comment line argument
     */
    public static void main(String[] args) {

        String directoryPath = "src/main/io";
        String extension = ".txt";
        String keyword = "standard";

        File rootDirectory = new File(directoryPath);

        if (!rootDirectory.exists() || !rootDirectory.isDirectory()) {
            System.out.println("Invalid directory path.");
            return;
        }

        System.out.println("Files with extension " + extension + ":\n");
        List<File> allFiles = getFilesByExtension(rootDirectory, extension);
        for (File file : allFiles) {
            printFileDetails(file);
        }

        System.out.println("Files containing the keyword '" + keyword + "':\n");
        searchKeywordInDirectory(rootDirectory, keyword);
    }
}