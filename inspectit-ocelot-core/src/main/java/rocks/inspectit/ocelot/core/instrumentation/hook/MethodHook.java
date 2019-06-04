package rocks.inspectit.ocelot.core.instrumentation.hook;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import rocks.inspectit.ocelot.bootstrap.context.InternalInspectitContext;
import rocks.inspectit.ocelot.bootstrap.instrumentation.IMethodHook;
import rocks.inspectit.ocelot.core.instrumentation.config.model.MethodHookConfiguration;
import rocks.inspectit.ocelot.core.instrumentation.context.ContextManager;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContextImpl;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Each {@link MethodHook} instances defines for a single method which actions are performed.
 * This defines for example which generic actions are executed or which metrics are collected.
 * {@link MethodHook}s are created, destroyed and mapped to methods via the {@link HookManager}.
 */
@Slf4j
@Data
@Builder
public class MethodHook implements IMethodHook {

    /**
     * The configuration on which this hook is based.
     * This object can be compared against newly derived configurations to see if the hook requires an update.
     */
    private final MethodHookConfiguration sourceConfiguration;

    /**
     * The context manager used to create the inspectit context.
     */
    private final ContextManager inspectitContextManager;

    /**
     * The list of actions to execute when the instrumented method is entered.
     */
    @Builder.Default
    private CopyOnWriteArrayList<IHookAction> entryActions = new CopyOnWriteArrayList<>();

    /**
     * The list of actions to execute when the instrumented method is exited.
     */
    @Builder.Default
    private CopyOnWriteArrayList<IHookAction> exitActions = new CopyOnWriteArrayList<>();

    /**
     * Stores details regarding the hooked method
     */
    private MethodReflectionInformation methodInformation;

    @Override
    public InternalInspectitContext onEnter(Object[] args, Object thiz) {
        val inspectitContext = inspectitContextManager.enterNewContext();
        val executionContext = new IHookAction.ExecutionContext(args, thiz, null, null, this, inspectitContext);

        for (val action : entryActions) {
            try {
                action.execute(executionContext);
            } catch (Throwable t) {
                log.error("Entry action {} executed for method {} threw an exception and from now on is disabled!",
                        action.getName(), methodInformation.getMethodFQN(), t);
                entryActions.remove(action);
            }
        }

        inspectitContext.makeActive();
        return inspectitContext;
    }

    @Override
    public void onExit(Object[] args, Object thiz, Object returnValue, Throwable thrown, InternalInspectitContext context) {
        val executionContext = new IHookAction.ExecutionContext(args, thiz, returnValue, thrown, this, (InspectitContextImpl) context);
        for (val action : exitActions) {
            try {
                action.execute(executionContext);
            } catch (Throwable t) {
                log.error("Exit action {} executed for method {} threw an exception and from now on is disabled!",
                        action.getName(), methodInformation.getMethodFQN(), t);
                exitActions.remove(action);
            }
        }
        context.close();
    }

}
