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
        searchDirectory(directory, visited, matchedFiles, extension, null); // keyword = null
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
     * Recursively searches the given directory and its subdirectories
     * for files containing the specified keyword.
     *
     * @param directory the root directory to start the search
     * @param keyword   the keyword to search for
     */
    public static void searchKeywordInDirectory(File directory, String keyword) {
        Set<Path> visited = new HashSet<>();
        searchDirectory(directory, visited, null, null, keyword); // extensionMatches = null
    }

    /**
     * Recursively traverses a directory and performs either or both of the following:
     * 1.Add files with a specific extension to the provided list
     * 2.Print the path of files containing a specific keyword.
     * Safely handles symbolic links and prevents infinite recursion.
     *
     * @param directory - the current directory to traverse
     * @param visited - a set of canonical paths of already visited directories
     * @param extensionMatches - the list to collect files by extension (nullable)
     * @param extension - the extension to filter files (nullable)
     * @param keyword - the keyword to search in files - null if keyword is not checked
     */
    private static void searchDirectory(File directory, Set<Path> visited, List<File> extensionMatches,
                                        String extension, String keyword) {
        try {
            Path realPath = directory.toPath().toRealPath();
            if (!visited.add(realPath)) return; // already visited
        } catch (IOException e) {
            return;
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
                searchDirectory(file, visited, extensionMatches, extension, keyword);
            } else if (file.isFile()) {
                if (extensionMatches != null && extension != null && file.getName().endsWith(extension)) {
                    extensionMatches.add(file);
                }

                // Search for keyword
                if (keyword != null) {
                    if (containsKeyword(file, keyword) || containsKeywordInBinary(file, keyword)) {
                        System.out.println("Keyword found in: " + file.getAbsolutePath());
                    }
                }
            }
        }
    }

    /**
     * Checks whether a binary file contains the specified keyword.
     * @param file - the binary file to be checked
     * @param keyword - the keyword to search for
     * @return rue if the keyword is found in the file, false otherwise
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

    public static void testBinarySearch() {
        // File path
        File binaryFile = new File("src/main/io/example.bin");
        String keyword = "sud";

        try {

            try (FileOutputStream fos = new FileOutputStream(binaryFile)) {
                String content = "This is a binary test file containing the word binary.";
                fos.write(content.getBytes());
            }

            boolean found = containsKeywordInBinary(binaryFile, keyword);
            System.out.println("\nKeyword found: " + found);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * The main function where the program starts
     * @param args - comment line argument
     */
    public static void main(String[] args) {

        String directoryPath = "src/main/io";
        String extension = ".txt";
        String keyword = "hello";

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

        testBinarySearch();
    }
}
