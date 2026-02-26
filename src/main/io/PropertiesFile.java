import java.io.*;
import java.util.*;

public class PropertiesFile {

    private final String path;
    private final Map<String, String> properties;

    /**
     * Constructs a PropertiesFile object and loads the properties from the specified file.
     * @param path the file path of the properties file
     * @throws IOException if an I/O error occurs while reading the file
     */
    public PropertiesFile(String path) throws IOException{
        this.path=path;
        this.properties = readProperties();
    }

    /**
     * Reads the properties file and loads all key-value pairs
     * into the map - key value pairs
     * @throws IOException if an I/O error occurs while reading the file
     * returns empty map if the file is missing
     */
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
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) continue;
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
        }return map;
    }

    public Map<String, String> getProperties() {
        return new HashMap<>(properties);
    }

    /**
     * Updates the value of an existing key or adds a new key-value pair
     * if the key does not already exist.
     *
     * @param key the property key
     * @param newValue the new value associated with the key
     */
    public void updateProperty(String key, String newValue) {
        if (properties.containsKey(key)) {
            System.out.println("Updated key: " + key + ", new value: " + newValue);
        } else {
            System.out.println("Added new key: " + key + ", value: " + newValue);
        }
        properties.put(key, newValue);
    }

    /**
     * Writes the properties map back to the file
     * @throws IOException if an I/O error occurs while writing to the file
     */
    public synchronized void writeProperties() throws IOException {
        File originalFile = new File(path);
        File tempFile = new File(path + ".tmp");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue());
                writer.newLine();
            }
        }
        if (!tempFile.renameTo(originalFile)) {
            throw new IOException("Could not replace original file");
        }
    }

    /**
     * Prints all properties
     */
    public void printProperties() {
        for (Map.Entry<String, String> entry : properties.entrySet()){
            System.out.println(entry.getKey() + " = " + entry.getValue());
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

        PropertiesFile config1= new PropertiesFile(path1);
        PropertiesFile config2= new PropertiesFile(path2);

        System.out.println("Properties from file1:");
        config1.printProperties();

        System.out.println("\nProperties from file2:");
        config2.printProperties();

        config1.updateProperty("address","Greendale");
        config1.updateProperty("age","21");

        config2.updateProperty("city","Mystic falls");
        config2.updateProperty("name","Elena");

        config1.writeProperties();
        config2.writeProperties();

        System.out.println("\nProperties after updates:");
        config1.printProperties();
        System.out.println("\n");
        config2.printProperties();
    }
}