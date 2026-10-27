package academy;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "benchmark",
        version = "1.0",
        mixinStandardHelpOptions = true,
        description = "Запуск бенчмарк-тестов для сравнения производительности вызовов методов")
public class Application implements Runnable {

    @Option(
            names = {"-t", "--test"},
            description = "Тип бенчмарка: string-concat, method-invoke, all")
    private String benchmarkType = "all";

    @Option(
            names = {"-o", "--output"},
            description = "Путь к файлу для сохранения результатов")
    private String outputPath;

    @Option(
            names = {"-f", "--format"},
            description = "Формат вывода: console, csv, markdown")
    private String format = "console";

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Application()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        try {
            System.out.println("🚀 Запуск бенчмарк-тестов...");
            System.out.println("Тип: " + benchmarkType);
            System.out.println("Формат вывода: " + format);

            OptionsBuilder optionsBuilder = new OptionsBuilder();

            switch (benchmarkType.toLowerCase()) {
                case "string-concat":
                    optionsBuilder.include(StringConcatBenchmark.class.getSimpleName());
                    break;
                case "method-invoke":
                    optionsBuilder.include(MethodInvocationBenchmark.class.getSimpleName());
                    break;
                case "all":
                default:
                    optionsBuilder.include("academy.*");
                    break;
            }

            Options opt =
                    optionsBuilder.shouldDoGC(true).shouldFailOnError(true).build();

            // Запуск бенчмарков
            String results = runBenchmarks(opt);

            // Сохранение результатов если указан путь
            if (outputPath != null) {
                saveResults(results);
            }

            System.out.println("✅ Бенчмарки успешно завершены!");

        } catch (Exception e) {
            System.err.println("❌ Ошибка при выполнении бенчмарков: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private String runBenchmarks(Options opt) throws RunnerException {

        Path tempFile;
        try {
            tempFile = Files.createTempFile("benchmark_results_", ".txt");
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать временный файл", e);
        }

        opt = new OptionsBuilder()
                .parent(opt)
                .output(tempFile.toAbsolutePath().toString())
                .forks(0)
                .build();

        new Runner(opt).run();

        try {
            Files.lines(tempFile).forEach(System.out::println);
            return String.join("\n", Files.readAllLines(tempFile));
        } catch (IOException e) {
            throw new RuntimeException("Не удалось прочитать результаты", e);
        } finally {

            try {
                boolean deleted = Files.deleteIfExists(tempFile);
                if (!deleted) {
                    System.err.println("Внимание: временный файл уже был удален: " + tempFile.toAbsolutePath());
                }
            } catch (IOException e) {
                System.err.println("Ошибка при удалении временного файла: " + e.getMessage());
            }
        }
    }

    private void saveResults(String results) {
        try (BufferedWriter writer = Files.newBufferedWriter(Path.of(outputPath))) {
            writer.write("Результаты бенчмарк-тестов\n");
            writer.write(
                    "Дата: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
            writer.write("==========================================\n\n");
            writer.write(results);
            System.out.println(
                    "📄 Результаты сохранены в: " + Path.of(outputPath).toAbsolutePath());
        } catch (IOException e) {
            System.err.println("❌ Не удалось сохранить результаты: " + e.getMessage());
        }
    }
}
