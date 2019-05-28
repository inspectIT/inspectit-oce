package rocks.inspectit.ocelot.core.instrumentation.config.model;

import lombok.*;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.InstrumentationRuleSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.RuleTracingSettings;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * This class represents an instrumentation rule defining a bunch of classes to instrument and to inject the dispatcher
 * hook which is used for generic data collection.
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode
@Builder(toBuilder = true)
public class InstrumentationRule {

    /**
     * The rule's name.
     */
    private String name;

    /**
     * The scope of this rule. This represents a matcher of types and methods that should be instrumented.
     */
    @Singular
    private Set<InstrumentationScope> scopes;

    @Singular
    private Collection<ActionCallConfig> preEntryActions;

    /**
     * The actions executed on the method entry.
     * The order of the actions in the list does not matter, they are ordered automatically by the
     * {@link rocks.inspectit.ocelot.core.instrumentation.hook.MethodHookGenerator}.
     */
    @Singular
    private Collection<ActionCallConfig> entryActions;

    @Singular
    private Collection<ActionCallConfig> postEntryActions;

    @Singular
    private Collection<ActionCallConfig> preExitActions;

    /**
     * The actions executed on the method exit.
     * The order of the actions in the list does not matter, they are ordered automatically by the
     * {@link rocks.inspectit.ocelot.core.instrumentation.hook.MethodHookGenerator}.
     */
    @Singular
    private Collection<ActionCallConfig> exitActions;

    @Singular
    private Collection<ActionCallConfig> postExitActions;

    /**
     * Maps metrics to the data keys or constants used as sources, see {@link InstrumentationRuleSettings#getMetrics()}.
     * However, this map is guaranteed to not contain null or blank values.
     * This means that disabled metrics have been filtered out.
     */
    @Singular
    private Map<String, String> metrics;

    /**
     * The tracing related settings.
     */
    @Builder.Default
    private RuleTracingSettings tracing = RuleTracingSettings.NO_TRACING_AND_ATTRIBUTES;
}
