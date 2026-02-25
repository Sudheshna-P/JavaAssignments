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
                        # this is a comment
                        """);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(Path.of(TEST_FILE));
//        Files.deleteIfExists(Path.of(TEST_FILE + ".tmp"));
    }

    @Test
    void testReadProperties() throws IOException {
        PropertiesFile config = new PropertiesFile(TEST_FILE);
        Map<String, String> props = config.getProperties();

        assertEquals("John", props.get("name"));
        assertEquals("30", props.get("age"));
        assertEquals("Paris", props.get("city"));
    }

    @Test
    void testUpdateExistingProperty() throws IOException {
        PropertiesFile config = new PropertiesFile(TEST_FILE);
        config.updateProperty("age", "31");

        assertEquals("31", config.getProperties().get("age"));
    }

    @Test
    void testAddNewProperty() throws IOException {
        PropertiesFile config = new PropertiesFile(TEST_FILE);
        config.updateProperty("country", "France");

        assertEquals("France", config.getProperties().get("country"));
    }

    @Test
    void testWriteProperties() throws IOException {
        PropertiesFile config = new PropertiesFile(TEST_FILE);
        config.updateProperty("age", "35");
        config.writeProperties();

        // Reload from disk to verify persistence
        PropertiesFile reloaded = new PropertiesFile(TEST_FILE);
        assertEquals("35", reloaded.getProperties().get("age"));
    }
}