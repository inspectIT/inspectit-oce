package rocks.inspectit.oce.core.instrumentation;

import net.bytebuddy.matcher.ElementMatchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import rocks.inspectit.oce.core.config.InspectitEnvironment;
import rocks.inspectit.oce.core.config.model.InspectitConfig;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.InternalSettings;
import rocks.inspectit.oce.core.instrumentation.config.InstrumentationConfigurationResolver;
import rocks.inspectit.oce.core.instrumentation.config.model.ClassInstrumentationConfiguration;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationRule;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationScope;
import rocks.inspectit.oce.core.instrumentation.event.ClassInstrumentedEvent;
import rocks.inspectit.oce.core.instrumentation.event.IClassDefinitionListener;
import rocks.inspectit.oce.core.instrumentation.special.SpecialSensor;
import rocks.inspectit.oce.core.selfmonitoring.SelfMonitoringService;
import rocks.inspectit.oce.core.testutils.DummyClassLoader;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AsyncClassTransformerUnitTest {

    @Mock
    InspectitEnvironment env;

    @Mock
    ApplicationContext ctx;

    @Mock
    Instrumentation instrumentation;

    @Mock
    SelfMonitoringService selfMonitoring;

    @Mock
    InstrumentationConfigurationResolver configResolver;

    @InjectMocks
    AsyncClassTransformer transformer = new AsyncClassTransformer();

    private static byte[] bytecodeOfTest;

    @BeforeAll
    static void readByteCode() {
        bytecodeOfTest = DummyClassLoader.readByteCode(AsyncClassTransformerUnitTest.class);
    }

    @BeforeEach
    void setupTransformer() {
        transformer.classDefinitionListeners = new ArrayList<>();
    }

    @Nested
    public class Init {

        @Test
        void testTransfomerSetup() {
            transformer.init();
            verify(instrumentation).addTransformer(transformer, true);
        }

    }

    @Nested
    public class Destroy {

        @Test
        void testTransfomerCleanup() throws Exception {

            InternalSettings internalSettings = new InternalSettings();
            internalSettings.setClassRetransformBatchSize(100);
            InstrumentationSettings settings = new InstrumentationSettings();
            settings.setInternal(internalSettings);
            InspectitConfig conf = new InspectitConfig();
            conf.setInstrumentation(settings);
            when(env.getCurrentConfig()).thenReturn(conf);

            SpecialSensor mockSensor = Mockito.mock(SpecialSensor.class);
            when(mockSensor.shouldInstrument(any(), any())).thenReturn(true);
            when(mockSensor.requiresInstrumentationChange(any(), any(), any())).thenReturn(true);
            when(mockSensor.instrument(any(), any(), any(), any())).then(invocation -> invocation.getArgument(3));

            InstrumentationScope scope = new InstrumentationScope(ElementMatchers.any(), ElementMatchers.any());
            InstrumentationRule rule = new InstrumentationRule(null, Collections.singleton(scope));

            ClassInstrumentationConfiguration mockedConfig = new ClassInstrumentationConfiguration(
                    Collections.singleton(mockSensor), Collections.singleton(rule), null
            );
            when(configResolver.getClassInstrumentationConfiguration(any())).thenReturn(mockedConfig);

            Class<AsyncClassTransformerUnitTest> clazz = AsyncClassTransformerUnitTest.class;
            String className = clazz.getName().replace('.', '/');
            transformer.transform(clazz.getClassLoader(), className, clazz, null, bytecodeOfTest);

            verify(mockSensor).instrument(any(), any(), any(), any());

            Mockito.reset(mockSensor);
            doAnswer((inv) -> transformer.transform(clazz.getClassLoader(), className, clazz, null, bytecodeOfTest))
                    .when(instrumentation).retransformClasses(clazz);

            transformer.destroy();

            verify(mockSensor, never()).instrument(any(), any(), any(), any());
            verify(instrumentation).retransformClasses(clazz);
            verify(instrumentation).removeTransformer(transformer);
        }


        @Test
        void testRetransformErrorHandling() throws Exception {

            InternalSettings internalSettings = new InternalSettings();
            internalSettings.setClassRetransformBatchSize(100);
            InstrumentationSettings settings = new InstrumentationSettings();
            settings.setInternal(internalSettings);
            InspectitConfig conf = new InspectitConfig();
            conf.setInstrumentation(settings);
            when(env.getCurrentConfig()).thenReturn(conf);

            DummyClassLoader loader = new DummyClassLoader();
            loader.loadCopiesOfClasses(AsyncClassTransformerUnitTest.class, FakeExecutor.class);

            transformer.instrumentedClasses.put(AsyncClassTransformerUnitTest.class, true);
            transformer.instrumentedClasses.put(FakeExecutor.class, true);

            doAnswer(invoc -> {
                Object[] definitons = invoc.getArguments();
                if (Arrays.stream(definitons).anyMatch(c -> c == FakeExecutor.class)) {
                    transformer.instrumentedClasses.invalidate(FakeExecutor.class);
                }
                if (Arrays.stream(definitons).anyMatch(c -> c == AsyncClassTransformerUnitTest.class)) {
                    throw new RuntimeException();
                }
                return null;
            }).when(instrumentation).retransformClasses(any());


            transformer.destroy();

            verify(instrumentation, times(1)).retransformClasses(any(), any());
            verify(instrumentation, times(3)).retransformClasses(any());
        }

    }

    @Nested
    public class Transform {

        @Test
        void verifyClassInstrumentedEventPublished() throws Exception {
            IClassDefinitionListener listener = Mockito.mock(IClassDefinitionListener.class);

            transformer.init();

            SpecialSensor mockSensor = Mockito.mock(SpecialSensor.class);
            when(mockSensor.instrument(any(), any(), any(), any())).then(invocation -> invocation.getArgument(3));
            InstrumentationScope scope = new InstrumentationScope(ElementMatchers.any(), ElementMatchers.any());
            InstrumentationRule rule = new InstrumentationRule(null, Collections.singleton(scope));

            ClassInstrumentationConfiguration mockedConfig = new ClassInstrumentationConfiguration(
                    Collections.singleton(mockSensor), Collections.singleton(rule), null
            );
            when(configResolver.getClassInstrumentationConfiguration(any())).thenReturn(mockedConfig);

            Class<AsyncClassTransformerUnitTest> clazz = AsyncClassTransformerUnitTest.class;
            String className = clazz.getName().replace('.', '/');
            transformer.transform(clazz.getClassLoader(), className, getClass(), null, bytecodeOfTest);

            verify(mockSensor).instrument(any(), any(), any(), any());
            verify(ctx).publishEvent(isA(ClassInstrumentedEvent.class));

        }

        @Test
        void testDefinitionListenersInvokedForNewClasses() throws Exception {
            IClassDefinitionListener listener = Mockito.mock(IClassDefinitionListener.class);
            transformer.classDefinitionListeners = Arrays.asList(listener);

            transformer.init();

            Class<AsyncClassTransformerUnitTest> clazz = AsyncClassTransformerUnitTest.class;
            String className = clazz.getName().replace('.', '/');
            ClassLoader loader = clazz.getClassLoader();
            transformer.transform(loader, className, null, null, bytecodeOfTest);

            verify(listener).onNewClassDefined(className, loader);

        }


        @Test
        void testDefinitionListenersNotInvokedForExistingClasses() throws Exception {
            IClassDefinitionListener listener = Mockito.mock(IClassDefinitionListener.class);
            transformer.classDefinitionListeners = Arrays.asList(listener);

            when(configResolver.getClassInstrumentationConfiguration(any()))
                    .thenReturn(ClassInstrumentationConfiguration.NO_INSTRUMENTATION);

            transformer.init();

            Class<AsyncClassTransformerUnitTest> clazz = AsyncClassTransformerUnitTest.class;
            String className = clazz.getName().replace('.', '/');
            ClassLoader loader = clazz.getClassLoader();
            transformer.transform(loader, className, getClass(), null, bytecodeOfTest);

            verify(listener, never()).onNewClassDefined(any(), any());
        }
    }

}
