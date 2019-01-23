package rocks.inspectit.oce.core.instrumentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.oce.core.config.InspectitEnvironment;
import rocks.inspectit.oce.core.config.model.InspectitConfig;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.InternalSettings;
import rocks.inspectit.oce.core.instrumentation.event.IClassDiscoveryListener;

import java.lang.instrument.Instrumentation;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NewClassDiscoveryServiceUnitTest {

    @Mock
    ScheduledExecutorService executor;
    Runnable scheduledRunnable = null;

    @Mock
    ScheduledFuture<?> scheduledFuture = null;

    @Mock
    Instrumentation instrumentation;

    @Mock
    InspectitEnvironment env;

    @Mock
    IClassDiscoveryListener mockListener;

    @InjectMocks
    NewClassDiscoveryService discovery = new NewClassDiscoveryService();

    private InternalSettings timingsConfiguration;

    @BeforeEach
    void setup() {
        when(executor.schedule(isA(Runnable.class), anyLong(), any())).then(invoc -> {
            scheduledRunnable = invoc.getArgument(0);
            return scheduledFuture;
        });
        discovery.listeners = Arrays.asList(mockListener);

        InspectitConfig conf = new InspectitConfig();
        InstrumentationSettings instr = new InstrumentationSettings();
        conf.setInstrumentation(instr);
        timingsConfiguration = new InternalSettings();
        instr.setInternal(timingsConfiguration);
        timingsConfiguration.setNewClassDiscoveryInterval(Duration.ofMillis(100));
        timingsConfiguration.setMinClassDefinitionDelay(Duration.ofMillis(0));
        timingsConfiguration.setMaxClassDefinitionDelay(Duration.ofMillis(1000));
        when(env.getCurrentConfig()).thenReturn(conf);
    }

    @Test
    void initialNotificationSentForAlreadyLoadedClasses() {
        HashSet<Class<?>> classes = new HashSet<>(Arrays.asList(String.class, Integer.class));
        when(instrumentation.getAllLoadedClasses()).thenReturn(classes.toArray(new Class[]{}));
        discovery.init();
        if (scheduledRunnable != null) {
            scheduledRunnable.run();
        }
        verify(mockListener, times(1)).onNewClassesDiscovered(eq(classes));
    }

    @Test
    void verifyTaskCanceledOnShutdown() {
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class[]{});
        discovery.init();
        discovery.destroy();
        verify(scheduledFuture, times(1)).cancel(anyBoolean());
    }


    @Test
    void newNotificationSentForNewClasses() {
        discovery.timestampMS = () -> 100L;
        HashSet<Class<?>> classes = new HashSet<>(Arrays.asList(String.class, Integer.class));
        when(instrumentation.getAllLoadedClasses()).thenReturn(classes.toArray(new Class[]{}));
        discovery.init();
        if (scheduledRunnable != null) {
            scheduledRunnable.run();
        }
        verify(mockListener, times(1)).onNewClassesDiscovered(eq(classes));

        discovery.onNewClassDefined(Long.class.getName(), null);
        classes.add(Long.class);
        when(instrumentation.getAllLoadedClasses()).thenReturn(classes.toArray(new Class[]{}));
        discovery.timestampMS = () -> 200L;

        if (scheduledRunnable != null) {
            scheduledRunnable.run();
        }
        verify(mockListener, times(1)).onNewClassesDiscovered(
                eq(new HashSet<>(Arrays.asList(Long.class))));
    }


    @Test
    void testInactivityAfterConfiguredTime() {
        discovery.timestampMS = () -> 100L;
        timingsConfiguration.setMinClassDefinitionDelay(Duration.ofMillis(10));

        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class[]{});
        discovery.init();
        if (scheduledRunnable != null) {
            Runnable r = scheduledRunnable;
            scheduledRunnable = null;
            r.run();
        }

        discovery.onNewClassDefined(null, null);
        Runnable r = scheduledRunnable;
        scheduledRunnable = null;
        r.run();

        Mockito.reset(instrumentation);
        discovery.timestampMS = () -> 10000L;
        r = scheduledRunnable;
        scheduledRunnable = null;
        r.run();

        verify(instrumentation, never()).getAllLoadedClasses();
    }


    @Test
    void testInactivityUntilConfiguredDelay() {
        discovery.timestampMS = () -> 100L;
        timingsConfiguration.setMinClassDefinitionDelay(Duration.ofMillis(200));

        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class[]{});
        discovery.init();
        if (scheduledRunnable != null) {
            Runnable r = scheduledRunnable;
            scheduledRunnable = null;
            r.run();
        }

        Mockito.reset(instrumentation);
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class[]{});

        discovery.onNewClassDefined(null, null);
        Runnable r = scheduledRunnable;
        scheduledRunnable = null;
        r.run();
        verify(instrumentation, never()).getAllLoadedClasses();

        discovery.timestampMS = () -> 500L;
        r = scheduledRunnable;
        scheduledRunnable = null;
        r.run();
        verify(instrumentation, times(1)).getAllLoadedClasses();
    }
}
