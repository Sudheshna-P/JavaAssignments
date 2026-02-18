import java.io.*;
import java.util.Objects;
/**
 *<p>Class Person implements Serializable interface so that the object of this
 * class can be serialized and deserialized</p>
 */
class Person implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String name;
    private final int age;
    private final String address;


    /**
     * Constructor which contains name, age and address of the person
     * all fields are immutable to ensure encapsulation
     * @param name of the person
     * @param age of the person
     * @param address of the person
     */
    public Person(String name, int age, String address ) {
        this.name = name;
        this.age = age;
        this.address = address;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (obj == null || getClass() != obj.getClass()) return false;

        Person other = (Person) obj;

        return age == other.age &&
                Objects.equals(name, other.name) &&
                Objects.equals(address, other.address);
    }


    /**
     *Returns string representation of the {@code Person}
     * @return string representation of the Person class
      */
    public String toString() {
        return "Person name = " + name + ", age = " + age + ", address= " + address;
    }
}

class FileSerialization {

    /**
     * Serializes the given object and writes it to the specified file.
     * @param obj the object to serialize; must implement {@link Serializable}
     * @param filename the name of the file to save the serialized object
     */
    public static void serialize(Object obj, String filename) throws IOException {
        try (FileOutputStream fileOut = new FileOutputStream(filename);
             ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(obj);
            System.out.println("Object has been serialized to " + filename);
        }
    }


    /**
     * Deserializes an object from the specified file.
     *
     * @param filename the name of the file containing the serialized object
     * @return the deserialized object, or {@code null} if an error occurs
     */
    public static Object deserialize(String filename) throws IOException, ClassNotFoundException{
        try (FileInputStream fileIn = new FileInputStream(filename);
             ObjectInputStream in = new ObjectInputStream(fileIn)) {
            Object obj = in.readObject();
            System.out.println("Object has been deserialized from " + filename);
            return obj;
        }
    }

    /**
     * The main method
     * @param args - comment line argument
     */
    public static void main(String[] args) {
        String filename = "person.ser";
        Person person = new Person("Sudheshna", 21, "Chennai");

        try {
            serialize(person, filename);

            Person deserializedPerson = (Person) deserialize(filename);

            System.out.println("Deserialized Person Details:");
            System.out.println(deserializedPerson);
            System.out.println(person.equals(deserializedPerson));

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
