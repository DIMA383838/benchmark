package academy;

import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(2)
public class MethodInvocationBenchmark {

    private Student student;
    private Method reflectionMethod;
    private MethodHandle methodHandle;
    private MethodHandle exactMethodHandle;
    private Supplier<String> lambdaSupplier;

    @Setup(Level.Trial)
    public void setUp() throws Throwable {
        student = new Student("John Doe", 21);

        reflectionMethod = Student.class.getMethod("name");
        reflectionMethod.setAccessible(true);

        MethodHandles.Lookup lookup = MethodHandles.lookup();
        methodHandle = lookup.findVirtual(Student.class, "name", MethodType.methodType(String.class));

        exactMethodHandle = methodHandle.asType(MethodType.methodType(String.class, Student.class));

        CallSite callSite = LambdaMetafactory.metafactory(
                lookup,
                "get",
                MethodType.methodType(Supplier.class, Student.class),
                MethodType.methodType(Object.class),
                methodHandle,
                MethodType.methodType(String.class));

        @SuppressWarnings("unchecked")
        Supplier<String> supplier =
                (Supplier<String>) callSite.getTarget().bindTo(student).invoke();
        lambdaSupplier = supplier;
    }

    @Benchmark
    public void directInvocation(Blackhole bh) {
        bh.consume(student.name());
    }

    @Benchmark
    public void reflectionInvocation(Blackhole bh) throws Exception {
        bh.consume(reflectionMethod.invoke(student));
    }

    @Benchmark
    public void methodHandleInvocation(Blackhole bh) throws Throwable {
        bh.consume(methodHandle.invoke(student));
    }

    @Benchmark
    public void lambdaMetafactoryInvocation(Blackhole bh) {
        bh.consume(lambdaSupplier.get());
    }

    @Benchmark
    public void directGetterInvocation(Blackhole bh) {
        bh.consume(student.getName());
    }
}
