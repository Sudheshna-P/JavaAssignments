import org.junit.jupiter.api.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PropertiesFileTest {

    private static final String TEST_FILE = "test.properties";

    @BeforeEach
    void setUp() throws IOException {
        Files.writeString(Path.of(TEST_FILE),
                """
                name=Sudheshna
                age=21
                city:Chennai
                country = India
                
                escapedKey\\=test=escapedValue
                # this is a comment
                ! another comment
                """);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(Path.of(TEST_FILE));
        Files.deleteIfExists(Path.of(TEST_FILE + ".tmp"));
    }

    /**
     * Test reading properties correctly
     * @throws IOException if any i/o error occurs
     */
    @Test
    void testReadProperties() throws IOException {
        PropertiesFile config = new PropertiesFile(TEST_FILE);
        Map<String, String> props = config.getProperties();

        assertEquals("Sudheshna", props.get("name"));
        assertEquals("21", props.get("age"));
        assertEquals("Chennai", props.get("city"));
        assertEquals("India", props.get("country"));
        assertEquals("escapedValue", props.get("escapedKey=test"));
    }

    /**
     * Test updating an existing key
     * @throws IOException if any i/o error occurs
     */
    @Test
    void testUpdateExistingProperty() throws IOException {
        PropertiesFile config = new PropertiesFile(TEST_FILE);
        config.updateProperty("age", "22");

        assertEquals("22", config.getProperties().get("age"));
    }

    /**
     * Test adding a new key
     * @throws IOException if any i/o error occurs
     */
    @Test
    void testAddNewProperty() throws IOException {
        PropertiesFile config = new PropertiesFile(TEST_FILE);
        config.updateProperty("state", "Tamil Nadu");

        assertEquals("Tamil Nadu", config.getProperties().get("state"));
    }

    /**
     * Test writing updates to disk
     * @throws IOException if any i/o error occurs
     */
    @Test
    void testWriteProperties() throws IOException {
        PropertiesFile config = new PropertiesFile(TEST_FILE);
        config.updateProperty("age", "30");
        config.writeProperties();

        // Reload from disk
        PropertiesFile reloaded = new PropertiesFile(TEST_FILE);
        assertEquals("30", reloaded.getProperties().get("age"));
    }

    /**
     * Test file not existing
     * @throws IOException if any i/o error occurs
     */
    @Test
    void testFileNotExists() throws IOException {
        String nonExistentFile = "missing.properties";
        Files.deleteIfExists(Path.of(nonExistentFile));

        PropertiesFile config = new PropertiesFile(nonExistentFile);
        assertTrue(config.getProperties().isEmpty());
    }


    /**
     * Test comments and empty lines are ignored
     * @throws IOException if any i/o error occurs
     */
    @Test
    void testCommentsIgnored() throws IOException {
        PropertiesFile config = new PropertiesFile(TEST_FILE);
        Map<String, String> props = config.getProperties();

        assertFalse(props.containsKey("# this is a comment"));
        assertFalse(props.containsKey("! another comment"));
    }

    /**
     * Test defensive copy (modifying returned map should not affect original)
     * @throws IOException if any i/o error occurs
     */
    @Test
    void testDefensiveCopy() throws IOException {
        PropertiesFile config = new PropertiesFile(TEST_FILE);
        Map<String, String> props = config.getProperties();

        props.put("hacked", "value");

        assertFalse(config.getProperties().containsKey("hacked"));
    }

    /**
     * Test multiple writes overwrite correctly
     * @throws IOException if any i/o error occurs
     */
    @Test
    void testMultipleWrites() throws IOException {
        PropertiesFile config = new PropertiesFile(TEST_FILE);
        config.updateProperty("age", "25");
        config.writeProperties();

        config.updateProperty("age", "26");
        config.writeProperties();

        PropertiesFile reloaded = new PropertiesFile(TEST_FILE);
        assertEquals("26", reloaded.getProperties().get("age"));
    }
}