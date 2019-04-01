package rocks.inspectit.ocelot.core.instrumentation.special;

import lombok.val;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationConfiguration;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Special sensor for passing the context via the {@link ScheduledExecutorService}.
 * This sensor will pass the context when using the following methods:
 * <p><ul>
 * <li>{@link ScheduledExecutorService#schedule(Runnable, long, TimeUnit)}
 * <li>{@link ScheduledExecutorService#schedule(Callable, long, TimeUnit)}
 * <li>{@link ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, TimeUnit)}
 * <li>{@link ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)}
 * </ul><p>
 */
@Component
public class ScheduledExecutorContextPropagationSensor implements SpecialSensor {

    private static final ElementMatcher<TypeDescription> CLASSES_MATCHER = isSubTypeOf(ScheduledExecutorService.class);

    @Override
    public boolean shouldInstrument(Class<?> clazz, InstrumentationConfiguration settings) {
        val type = TypeDescription.ForLoadedType.of(clazz);
        return settings.getSource().getSpecial().isScheduledExecutorContextPropagation() && CLASSES_MATCHER.matches(type);
    }

    @Override
    public boolean requiresInstrumentationChange(Class<?> clazz, InstrumentationConfiguration first, InstrumentationConfiguration second) {
        return false; //if the sensor stays active it never requires changes
    }

    @Override
    public DynamicType.Builder instrument(Class<?> clazz, InstrumentationConfiguration settings, DynamicType.Builder builder) {
        return builder
                .visit(ScheduledExecutorRunnableAdvice.TARGET)
                .visit(ScheduledExecutorCallableAdvice.TARGET);
    }

    /**
     * Advice for wrapping the first method argument - which has to be a {@link Runnable} - into a Runnable for attaching and detaching the current context.
     * See also {@link io.grpc.Context#wrap(Runnable)}
     */
    private static class ScheduledExecutorRunnableAdvice {

        static final AsmVisitorWrapper.ForDeclaredMethods TARGET = Advice.to(ScheduledExecutorRunnableAdvice.class)
                .on(nameStartsWith("schedule").and(takesArgument(0, Runnable.class)));

        @Advice.OnMethodEnter
        public static void onMethodEnter(@Advice.Argument(value = 0, readOnly = false) Runnable runnable) {
            runnable = Instances.contextManager.wrap(runnable);
        }
    }

    /**
     * Advice for wrapping the first method argument - which has to be a {@link Callable} - into a Callback for attaching and detaching the current context.
     * See also {@link io.grpc.Context#wrap(Callable)}
     */
    private static class ScheduledExecutorCallableAdvice {

        static final AsmVisitorWrapper.ForDeclaredMethods TARGET = Advice.to(ScheduledExecutorCallableAdvice.class)
                .on(named("schedule").and(takesArgument(0, Callable.class)));

        @Advice.OnMethodEnter
        public static void onMethodEnter(@Advice.Argument(value = 0, readOnly = false) Callable callable) {
            callable = Instances.contextManager.wrap(callable);
        }
    }
}
