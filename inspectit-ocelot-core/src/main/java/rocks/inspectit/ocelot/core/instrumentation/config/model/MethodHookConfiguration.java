package rocks.inspectit.ocelot.core.instrumentation.config.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.RuleTracingSettings;
import rocks.inspectit.ocelot.core.instrumentation.hook.MethodHook;

import java.util.List;
import java.util.Map;

/**
 * The configuration used to build a {@link MethodHook}
 * Note that the {@link #equals(Object)} function on this class is used to decide whether a recreation of the hook is required.
 */
@Builder
@Value
public class MethodHookConfiguration {

    /**
     * The combined tracing settings from all rules matching this method.
     */
    @Builder.Default
    private RuleTracingSettings tracing = RuleTracingSettings.NO_TRACING_AND_ATTRIBUTES;

    /**
     * Holds all actions executed in the specified order directly before the ones in {@link #entryActions}.
     */
    @Singular
    private List<ActionCallConfig> preEntryActions;

    /**
     * The ordered list of data assignments performed on method entry.
     */
    @Singular
    private List<ActionCallConfig> entryActions;

    /**
     * Holds all actions executed in the specified order directly after the ones in {@link #entryActions}.
     * These actions are executed after a span has been started or continued, if requested.
     */
    @Singular
    private List<ActionCallConfig> postEntryActions;

    /**
     * Holds all actions executed in the specified order directly before the ones in {@link #exitActions}.
     */
    @Singular
    private List<ActionCallConfig> preExitActions;

    /**
     * The ordered list of data assignments performed on method exit.
     */
    @Singular
    private List<ActionCallConfig> exitActions;

    /**
     * Holds all actions executed in the specified order directly after the ones in {@link #entryActions}.
     * These actions are executed after a span has been finished, if requested.
     */
    @Singular
    private List<ActionCallConfig> postExitActions;

    /**
     * Maps the metrics to capture to the data keys to use as value.
     */
    @Singular
    private Map<String, String> dataMetrics;

    /**
     * Maps the metrics to capture to the constants which should be used as value.
     * This is for example useful for counter metrics where you don't actually are interested in the value.
     */
    @Singular
    private Map<String, Number> constantMetrics;
}
