package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.logging.Handler;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

    private final Clock clock;
    private final ProfilingState state = new ProfilingState();
    private final ZonedDateTime startTime;

    @Inject
    ProfilerImpl(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
        this.startTime = ZonedDateTime.now(clock);
    }

    @Override
    public <T> T wrap(Class<T> klass, T delegate) {
        // TODO: Use a dynamic proxy (java.lang.reflect.Proxy) to "wrap" the delegate in a
        //       ProfilingMethodInterceptor and return a dynamic proxy from this method.
        //       See https://docs.oracle.com/javase/10/docs/api/java/lang/reflect/Proxy.html.

        if (klass.getDeclaredMethods().length == 0) {
            throw new IllegalArgumentException("List of methods is empty");
        }

        boolean profiled = false;
        for (Method method : klass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Profiled.class)) {
                profiled = true;
                break;
            }
        }

        if(!profiled) {
            throw new IllegalArgumentException(klass.getName() + " has no @Profiled Annotated Method included");
        }

        Object proxy =
                Proxy.newProxyInstance(
                        ProfilerImpl.class.getClassLoader(),
                        new Class<?>[]{Objects.requireNonNull(klass)},
                        new ProfilingMethodInterceptor(clock, state, delegate));
        //noinspection unchecked
        return (T) proxy;
    }

    @Override
    public void writeData(Path path) {
        // TODO: Write the ProfilingState data to the given file path. If a file already exists at that
        //       path, the new data should be appended to the existing file.
        try (Writer writer = new FileWriter(path.toFile(), true)) {
            writeData(writer);
        } catch (Exception e) {
            e.getLocalizedMessage();
        }
    }

    @Override
    public void writeData(Writer writer) throws IOException {
        writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
        writer.write(System.lineSeparator());
        state.write(writer);
        writer.write(System.lineSeparator());
    }
}
