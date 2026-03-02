import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class PropertiesFile {

    private final String path;
    private final Map<String, String> properties;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public PropertiesFile(String path) throws IOException {
        this.path = path;
        this.properties = new HashMap<>(readProperties());
    }

    private Map<String, String> readProperties() throws IOException {
        Map<String, String> map = new HashMap<>();
        File file = new File(path);

        if (!file.exists()) {
            return map;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#") || line.startsWith("!"))
                    continue;

                int index = -1;
                boolean escaped = false;

                for (int i = 0; i < line.length(); i++) {
                    char ch = line.charAt(i);

                    if (ch == '\\') {
                        escaped = !escaped;
                        continue;
                    }
                    if (!escaped && (ch == '=' || ch == ':')) {
                        index = i;
                        break;
                    }

                    escaped = false;
                }
                if (index == -1) {
                    for (int i = 0; i < line.length(); i++) {
                        if (Character.isWhitespace(line.charAt(i))) {
                            index = i;
                            break;
                        }
                    }
                }
                if (index > 0) {
                    String key = line.substring(0, index).trim();
                    key = key.replace("\\=", "=").replace("\\:", ":");
                    String value = line.substring(index + 1).trim();
                    map.put(key, value);
                }
            }
        }

        return map;
    }

    public Map<String, String> getProperties() {
        lock.readLock().lock();
        try {
            return new HashMap<>(properties);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void updateProperty(String key, String newValue) {
        lock.writeLock().lock();
        try {
            properties.put(key, newValue);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void writeProperties() throws IOException {
        lock.writeLock().lock();
        try {
            Path originalPath = Path.of(path);
            Path tempPath = Path.of(path + ".tmp");

            try (BufferedWriter writer = Files.newBufferedWriter(tempPath)) {
                for (Map.Entry<String, String> entry : properties.entrySet()) {
                    writer.write(entry.getKey() + "=" + entry.getValue());
                    writer.newLine();
                }
            }

            Files.move(tempPath, originalPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

        } finally {
            lock.writeLock().unlock();
        }
    }

    public void printProperties() {
        lock.readLock().lock();
        try {
            properties.forEach((k, v) ->
                    System.out.println(k + " = " + v)
            );
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * The main function
     * @param args - comment line argument
     * @throws IOException - when input or output error occurs
     */
    public static void main(String[] args) throws IOException {
        String path1 = "/home/sudheshna/IdeaProjects/JavaAssignments/src/main/io/config.properties";
        String path2 = "/home/sudheshna/IdeaProjects/JavaAssignments/src/main/io/config2.properties";

        PropertiesFile config1 = new PropertiesFile(path1);
        PropertiesFile config2 = new PropertiesFile(path2);

        System.out.println("Properties from file1:");
        config1.printProperties();

        System.out.println("\nProperties from file2:");
        config2.printProperties();

        config1.updateProperty("address", "Greendale");
        config1.updateProperty("age", "21");

        config2.updateProperty("city", "Mystic falls");
        config2.updateProperty("name", "Elena");

        config1.writeProperties();
        config2.writeProperties();

        System.out.println("\nProperties after updates:");
        config1.printProperties();
        System.out.println("\n");
        config2.printProperties();
    }
}

