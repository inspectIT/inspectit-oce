package rocks.inspectit.ocelot.core.instrumentation.config;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.RuleTracingSettings;
import rocks.inspectit.ocelot.core.instrumentation.config.model.ActionCallConfig;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationConfiguration;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationRule;
import rocks.inspectit.ocelot.core.instrumentation.config.model.MethodHookConfiguration;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class MethodHookConfigurationResolver {

    @Autowired
    GenericActionCallSorter scheduler;

    /**
     * Derives the configuration of the hook for the given method.
     *
     * @param allSettings  The global instrumentation configuration, used for the global master switches
     * @param matchedRules All enabled rules which have a scope which matches to this method, must contain at least one value
     * @return
     */
    public MethodHookConfiguration buildHookConfiguration(InstrumentationConfiguration allSettings, Set<InstrumentationRule> matchedRules)
            throws Exception {

        val result = MethodHookConfiguration.builder();
        result.entryActions(combineAndOrderActionCalls(matchedRules, InstrumentationRule::getEntryActions));
        result.exitActions(combineAndOrderActionCalls(matchedRules, InstrumentationRule::getExitActions));

        if (allSettings.isMetricsEnabled()) {
            resolveMetrics(result, matchedRules);
        }

        if (allSettings.isTracingEnabled()) {
            resolveTracing(result, matchedRules);
        }

        return result.build();
    }

    private void resolveTracing(MethodHookConfiguration.MethodHookConfigurationBuilder result, Set<InstrumentationRule> matchedRules) throws ConflictingDefinitionsException {

        val builder = RuleTracingSettings.builder();

        Set<InstrumentationRule> tracingRules = matchedRules.stream()
                .filter(r -> r.getTracing() != null)
                .collect(Collectors.toSet());

        if (!tracingRules.isEmpty()) {

            resolveStartSpan(tracingRules, builder);
            resolveEndSpan(tracingRules, builder);
            resolveContinueSpan(tracingRules, builder);
            builder.storeSpan(getAndDetectConflicts(tracingRules, r -> r.getTracing().getStoreSpan(), s -> !StringUtils.isEmpty(s), "store span data key"));
            resolveSpanAttributeWriting(tracingRules, builder);

            result.tracing(builder.build());
        }

    }

    private void resolveSpanAttributeWriting(Set<InstrumentationRule> matchedRules, RuleTracingSettings.RuleTracingSettingsBuilder builder) throws ConflictingDefinitionsException {
        Collection<InstrumentationRule> attributeWritingRules = matchedRules.stream()
                .filter(r -> !r.getTracing().getAttributes().isEmpty())
                .collect(Collectors.toSet());
        if (!attributeWritingRules.isEmpty()) {
            Set<String> writtenAttributes = attributeWritingRules.stream()
                    .flatMap(r -> r.getTracing().getAttributes().entrySet().stream())
                    .filter(e -> !StringUtils.isEmpty(e.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());

            Map<String, String> resultAttributes = new HashMap<>();
            for (String attributeKey : writtenAttributes) {
                String dataKey = getAndDetectConflicts(attributeWritingRules, r -> r.getTracing().getAttributes().get(attributeKey),
                        x -> !StringUtils.isEmpty(x), "the span attribute'" + attributeKey + "'");
                resultAttributes.put(attributeKey, dataKey);
            }
            builder.attributes(resultAttributes);
            builder.attributeConditions(getAndDetectConflicts(attributeWritingRules, r -> r.getTracing().getAttributeConditions(), x -> true, "span attribute writing conditions"));
        }
    }

    private void resolveContinueSpan(Set<InstrumentationRule> matchedRules, RuleTracingSettings.RuleTracingSettingsBuilder builder) throws ConflictingDefinitionsException {
        String continueSpan = getAndDetectConflicts(matchedRules, r -> r.getTracing().getContinueSpan(), x -> true, "continue-span");
        builder.continueSpan(continueSpan);
        if (continueSpan != null) {
            builder.continueSpanConditions(getAndDetectConflicts(matchedRules, r -> r.getTracing().getContinueSpanConditions(), x -> true, "continue span conditions"));
        }
    }

    private void resolveEndSpan(Set<InstrumentationRule> matchedRules, RuleTracingSettings.RuleTracingSettingsBuilder builder) throws ConflictingDefinitionsException {
        boolean endSpan = getAndDetectConflicts(matchedRules, r -> r.getTracing().isEndSpan(), x -> true, "end-span");
        builder.endSpan(endSpan);
        if (endSpan) {
            builder.endSpanConditions(getAndDetectConflicts(matchedRules, r -> r.getTracing().getEndSpanConditions(), x -> true, "end span conditions"));
        }
    }

    private void resolveStartSpan(Set<InstrumentationRule> matchedRules, RuleTracingSettings.RuleTracingSettingsBuilder builder) throws ConflictingDefinitionsException {
        boolean startSpan = getAndDetectConflicts(matchedRules, r -> r.getTracing().isStartSpan(), x -> true, "start-span");
        builder.startSpan(startSpan);
        if (startSpan) {
            builder.name(getAndDetectConflicts(matchedRules, r -> r.getTracing().getName(), n -> !StringUtils.isEmpty(n), "the span name"));
            builder.kind(getAndDetectConflicts(matchedRules, r -> r.getTracing().getKind(), Objects::nonNull, "the span kind"));
            builder.startSpanConditions(getAndDetectConflicts(matchedRules, r -> r.getTracing().getStartSpanConditions(), x -> true, "start span conditions"));
        }
    }


    /**
     * Utility function for merging configurations from multiple rules and detecting conflicts.
     * This method first calls the given getter on all specified rules and filters the results using the given filter.
     * <p>
     * It then ensures that all provided values are equal, otherwise raises an exception with the given message
     *
     * @param rules            the rules on which the getter will be called
     * @param getter           the getter function to call on each rule
     * @param filter           the predicate to filter the results of the getters with, e.g. Objects#nonNull
     * @param exceptionMessage the name of the setting to print in an exception message
     * @param <T>              the type of the value which is being queried
     * @return null if none of the rules have a setting matching the given filter. Otherwise returns the setting found.
     * @throws ConflictingDefinitionsException thrown if a conflicting setting is detected
     */
    private <T> T getAndDetectConflicts(Collection<InstrumentationRule> rules, Function<InstrumentationRule, T> getter, Predicate<T> filter, String exceptionMessage)
            throws ConflictingDefinitionsException {

        Optional<InstrumentationRule> firstMatch = rules.stream().filter(r -> filter.test(getter.apply(r))).findFirst();
        if (firstMatch.isPresent()) {
            T value = getter.apply(firstMatch.get());
            Optional<InstrumentationRule> secondMatch = rules.stream()
                    .filter(r -> r != firstMatch.get())
                    .filter(r -> filter.test(getter.apply(r)))
                    .filter(r -> !Objects.equals(getter.apply(r), getter.apply(firstMatch.get())))
                    .findFirst();
            if (secondMatch.isPresent()) {
                throw new ConflictingDefinitionsException(firstMatch.get(), secondMatch.get(), exceptionMessage);
            } else {
                return value;
            }
        } else {
            return null;
        }
    }

    /**
     * Combines all metric definitions from the given rules
     *
     * @param result       the hook configuration to which the measurement definitions are added
     * @param matchedRules the rules to combine
     * @throws ConflictingDefinitionsException of two rules define different values for the same metric
     */
    private void resolveMetrics(MethodHookConfiguration.MethodHookConfigurationBuilder result, Set<InstrumentationRule> matchedRules) throws ConflictingDefinitionsException {

        Map<String, InstrumentationRule> metricDefinitions = new HashMap<>();
        for (val rule : matchedRules) {
            //check for conflicts first
            for (val metricName : rule.getMetrics().keySet()) {
                if (metricDefinitions.containsKey(metricName)) {
                    throw new ConflictingDefinitionsException(metricDefinitions.get(metricName), rule, "the metric '" + metricName + "'");
                }
                metricDefinitions.put(metricName, rule);
            }
            rule.getMetrics().forEach((name, value) -> {
                try {
                    double constantValue = Double.parseDouble(value);
                    result.constantMetric(name, constantValue);
                } catch (NumberFormatException e) {
                    //the specified value is not a double value, we therefore assume it is a data key
                    result.dataMetric(name, value);
                }
            });
        }
    }

    /**
     * Combines and correctly orders all action calls from the given rules to a single map
     *
     * @param rules         the rules whose generic action calls should be merged
     * @param actionsGetter the getter to access the rules to process, e.g. {@link InstrumentationRule#getEntryActions()}
     * @return a map mapping the data keys to the action call which define the values
     * @throws ConflictingDefinitionsException                       if the same data key is defined with different generic action calls
     * @throws GenericActionCallSorter.CyclicDataDependencyException if the action calls have cyclic dependencies preventing a scheduling
     */
    private List<ActionCallConfig> combineAndOrderActionCalls(Set<InstrumentationRule> rules, Function<InstrumentationRule, Collection<ActionCallConfig>> actionsGetter)
            throws ConflictingDefinitionsException, GenericActionCallSorter.CyclicDataDependencyException {
        Map<String, InstrumentationRule> dataOrigins = new HashMap<>();
        Map<String, ActionCallConfig> dataDefinitions = new HashMap<>();
        for (val rule : rules) {
            Collection<ActionCallConfig> actions = actionsGetter.apply(rule);
            for (val dataDefinition : actions) {
                String dataKey = dataDefinition.getName();

                //check if we have previously already encountered a differing definition for the key
                if (dataOrigins.containsKey(dataKey) && !dataDefinition.equals(dataDefinitions.get(dataKey))) {
                    throw new ConflictingDefinitionsException(dataOrigins.get(dataKey), rule, "the data key '" + dataKey + "'");
                }

                dataOrigins.put(dataKey, rule);
                dataDefinitions.put(dataKey, dataDefinition);
            }
        }

        return scheduler.orderActionCalls(dataDefinitions.values());
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    static class ConflictingDefinitionsException extends Exception {
        /**
         * The first rule assigning a value to dataKey
         */
        private InstrumentationRule first;
        /**
         * The second rule assigning a value to dataKey
         */
        private InstrumentationRule second;

        private String messageSuffix;

        @Override
        public String getMessage() {
            return "The rules '" + first.getName() + "' and '" + second.getName() + "' contain conflicting definitions for " + messageSuffix;
        }
    }
}
