package academy;

import java.lang.reflect.Field;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "class-inspector",
        version = "1.0",
        mixinStandardHelpOptions = true,
        description = "Analyzes Java classes and generates instances")
public class Application implements Runnable {

    @Option(
            names = {"-c", "--class"},
            required = true,
            description = "Fully qualified class name")
    private String className;

    @Option(
            names = {"-f", "--format"},
            defaultValue = "TEXT",
            description = "Output format: TEXT or JSON")
    private String format;

    @Option(
            names = {"--create"},
            description = "Create and print instance of the class")
    private boolean createInstance;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Application()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        try {
            Class<?> clazz = Class.forName(className);

            if (createInstance) {
                Object instance = ClassInspector.create(clazz);
                // Выводим информацию о созданном экземпляре
                System.out.println("Created instance of: " + clazz.getSimpleName());
                System.out.println("Instance details: " + instance);

                // Дополнительно выводим значения полей
                System.out.println("Field values:");
                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);
                    System.out.println("  " + field.getName() + ": " + field.get(instance));
                }
            } else {
                String result = ClassInspector.inspect(clazz, format.toUpperCase());
                System.out.println(result);
            }

            System.exit(0);

        } catch (ClassNotFoundException e) {
            System.err.println("Class not found: " + className);
            System.exit(2);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid format: " + format + ". Supported formats: TEXT, JSON");
            System.exit(2);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
