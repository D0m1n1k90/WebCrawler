package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

    private final Clock clock;
    private final ProfilingState profilingState;
    private final Object delegate;

    // TODO: You will need to add more instance fields and constructor arguments to this class.
    ProfilingMethodInterceptor(Clock clock, ProfilingState profilingState, Object delegate) {
        this.clock = Objects.requireNonNull(clock);
        this.profilingState = profilingState;
        this.delegate = delegate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // TODO: This method interceptor should inspect the called method to see if it is a profiled
        //       method. For profiled methods, the interceptor should record the start time, then
        //       invoke the method using the object that is being profiled. Finally, for profiled
        //       methods, the interceptor should record how long the method call took, using the
        //       ProfilingState methods.

        Instant startTime = clock.instant();
        Object invokedMethod = null;
        try {
            invokedMethod = method.invoke(this.delegate, args);
            if (method.isAnnotationPresent(Profiled.class)) {
                calcAndSetDuration(startTime, method);
            }
            return invokedMethod;
        } catch (InvocationTargetException e) {
            calcAndSetDuration(startTime, method);
            throw e.getTargetException();
        } catch (IllegalAccessException e) {
            calcAndSetDuration(startTime, method);
            throw new RuntimeException(e);
        }

    }

    private void calcAndSetDuration(Instant startTime, Method method) {
        Duration duration = Duration.between(startTime, clock.instant());
        profilingState.record(this.delegate.getClass(), method, duration);
    }
}
