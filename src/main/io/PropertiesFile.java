import java.io.*;
import java.util.*;

public class PropertiesFile {

    /**
     * Reads a properties and returns a map of key-value pairs
     * @param path - path of the properties file
     * @return the map of key value pairs
     * @throws IOException when there is an error in input or output
     */
    public static Map<String, String> readProperties(String path) throws IOException {
        Map<String, String> map = new HashMap<>();
        BufferedReader reader = new BufferedReader(new FileReader(path));
        String line;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            int index = line.indexOf('=');
            if (index > 0) {
                String key = line.substring(0, index).trim();
                String value = line.substring(index + 1).trim();
                map.put(key, value);
            }
        }
        reader.close();
        return map;
    }


    /**
     * Prints all properties
     * @param props it is the map of key - value pair
     */
    public static void printProperties(Map<String, String> props) {
        for (Map.Entry<String, String> entry : props.entrySet()) {
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }
    }

    /**
     * Updates a property or adds a new one if it doesn't exist
     * @param filePath the path of the file
     * @param key the key whose value is updated
     * @param newValue the value that is given to the key
     * @throws IOException if error in input or output
     */
    public static void updateProperty(String filePath, String key, String newValue) throws IOException {
        Map<String, String> props = readProperties(filePath);
        if (props.containsKey(key)) {
            System.out.println("Updated key: " + key + ", new value: " + newValue);
        } else {
            System.out.println("Added new key: " + key + ", value: " + newValue);
        }
        props.put(key, newValue);

        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
        for (Map.Entry<String, String> entry : props.entrySet()) {
            writer.write(entry.getKey() + "=" + entry.getValue());
            writer.newLine();
        }
        writer.close();
    }

    /**
     * The main function
     * @param args comment line argument
     * @throws IOException when any error in input or output
     */
    public static void main(String[] args) throws IOException {
        String path = "/home/sudheshna/IdeaProjects/JavaAssignments/src/main/io/config.properties";

        Map<String, String> props = readProperties(path);
        System.out.println("Properties from file:");
        printProperties(props);

        updateProperty(path, "address", "Chennai");
        updateProperty(path, "msg", "hello");
        updateProperty(path, "age", "21");
        updateProperty(path, "name", "Sudheshna");
        updateProperty(path, "newKey", "newValue"); // adding a completely new key

    }
}
