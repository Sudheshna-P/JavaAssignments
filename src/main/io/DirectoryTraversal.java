import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
     * Safely handles symbolic links to avoid infinite recursion.
     */
    public static List<File> getFilesByExtension(File directory, String extension) {
        List<File> matchedFiles = new ArrayList<>();
        Set<Path> visited = new HashSet<>();
        collectFiles(directory, extension, matchedFiles, visited);
        return matchedFiles;
    }

    private static void collectFiles(File directory, String extension, List<File> matchedFiles, Set<Path> visited) {
        try {
            Path realPath = directory.toPath().toRealPath(); // resolves symlinks
            if (!visited.add(realPath)) return; // already visited, avoid loops
        } catch (IOException e) {
            System.err.println("Cannot resolve path: " + directory);
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            try {
                Path filePath = file.toPath();
                if (Files.isSymbolicLink(filePath)) continue; // skip symbolic links
            } catch (Exception e) {
                continue; // skip problematic files
            }

            if (file.isDirectory()) {
                collectFiles(file, extension, matchedFiles, visited);
            } else if (file.isFile() && file.getName().endsWith(extension)) {
                matchedFiles.add(file);
            }
        }
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
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Recursively searches all files in the directory for the given word.
     * Safely handles symbolic links.
     */
    public static void searchKeywordInDirectory(File directory, String keyword) {
        Set<Path> visited = new HashSet<>();
        searchDirectory(directory, keyword, visited);
    }

    private static void searchDirectory(File directory, String keyword, Set<Path> visited) {
        try {
            Path realPath = directory.toPath().toRealPath();
            if (!visited.add(realPath)) return; // already visited
        } catch (IOException e) {
            return; // skip directories that can't be resolved
        }

        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            try {
                Path filePath = file.toPath();
                if (Files.isSymbolicLink(filePath)) continue; // skip symbolic links
            } catch (Exception e) {
                continue;
            }

            if (file.isDirectory()) {
                searchDirectory(file, keyword, visited);
            } else if (file.isFile()) {
                if (containsKeyword(file, keyword) || containsKeywordInBinary(file, keyword)) {
                    System.out.println("Keyword found in: " + file.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Checks whether the binary file contains the keyword.
     * Safely handles broken symlinks.
     */
    public static boolean containsKeywordInBinary(File file, String keyword) {
        byte[] keywordBytes = keyword.getBytes();

        try {
            Path path = file.toPath();
            if (Files.isSymbolicLink(path)) return false; // skip symbolic links
            if (!Files.exists(path) || !Files.isRegularFile(path)) return false; // skip non-existent or special files

            byte[] fileBytes = Files.readAllBytes(path);

            for (int i = 0; i <= fileBytes.length - keywordBytes.length; i++) {
                boolean match = true;
                for (int j = 0; j < keywordBytes.length; j++) {
                    if (fileBytes[i + j] != keywordBytes[j]) {
                        match = false;
                        break;
                    }
                }
                if (match) return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
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
