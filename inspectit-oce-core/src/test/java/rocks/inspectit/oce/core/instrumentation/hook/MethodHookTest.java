package rocks.inspectit.oce.core.instrumentation.hook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.oce.bootstrap.context.IInspectitContext;
import rocks.inspectit.oce.core.instrumentation.context.ContextManager;

import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MethodHookTest {

    @Mock
    private IInspectitContext context;

    @Mock
    private ContextManager contextManager;

    @BeforeEach
    void setupContextManagerMock() {
        when(contextManager.enterNewContext()).thenReturn(context);
    }

    @Nested
    class OnEnter {

        @Test
        void testExceptionHandling() {
            IHookAction first = Mockito.mock(IHookAction.class);
            IHookAction second = Mockito.mock(IHookAction.class);
            IHookAction third = Mockito.mock(IHookAction.class);
            doThrow(Error.class).when(second).execute(any());
            MethodHook hook = MethodHook.builder()
                    .inspectitContextManager(contextManager)
                    .entryActions(new CopyOnWriteArrayList<>(Arrays.asList(first, second, third)))
                    .build();

            IInspectitContext ctx = hook.onEnter(null, null);
            assertThat(ctx).isSameAs(context);
            verify(context, times(1)).makeActive();
            verify(context, times(0)).close();

            verify(first, times(1)).execute(any());
            verify(second, times(1)).execute(any());
            verify(third, times(1)).execute(any());

            hook.onExit(null, null, null, null, ctx);
            verify(context, times(1)).close();

            ctx = hook.onEnter(null, null);

            verify(first, times(2)).execute(any());
            verify(second, times(1)).execute(any());
            verify(third, times(2)).execute(any());
        }

    }

    @Nested
    class OnExit {

        @Test
        void testExceptionHandling() {
            IHookAction first = Mockito.mock(IHookAction.class);
            IHookAction second = Mockito.mock(IHookAction.class);
            IHookAction third = Mockito.mock(IHookAction.class);
            doThrow(Error.class).when(second).execute(any());
            MethodHook hook = MethodHook.builder()
                    .inspectitContextManager(contextManager)
                    .exitActions(new CopyOnWriteArrayList<>(Arrays.asList(first, second, third)))
                    .build();

            IInspectitContext ctx = hook.onEnter(null, null);
            hook.onExit(null, null, null, null, ctx);

            verify(first, times(1)).execute(any());
            verify(second, times(1)).execute(any());
            verify(third, times(1)).execute(any());

            ctx = hook.onEnter(null, null);
            hook.onExit(null, null, null, null, ctx);

            verify(first, times(2)).execute(any());
            verify(second, times(1)).execute(any());
            verify(third, times(2)).execute(any());
        }

    }

}
