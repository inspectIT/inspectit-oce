package rocks.inspectit.oce.core.instrumentation.config;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.dataproviders.DataProviderCallSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.rules.InstrumentationRuleSettings;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationRule;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationScope;
import rocks.inspectit.oce.core.instrumentation.config.model.ResolvedDataProviderCall;
import rocks.inspectit.oce.core.instrumentation.config.model.ResolvedGenericDataProviderConfig;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is used to resolve the {@link InstrumentationRule}s which are defined by {@link InstrumentationRuleSettings}
 * contained in the configuration.
 */
@Component
@Slf4j
public class InstrumentationRuleResolver {

    @Autowired
    private InstrumentationScopeResolver scopeResolver;

    /**
     * Creates a set containing {@link InstrumentationRule}s which are based on the {@link InstrumentationRuleSettings}
     * contained in the given {@link InstrumentationSettings}.
     *
     * @param source the configuration which is used as basis for the rules
     * @return A set containing the resolved rules.
     */
    public Set<InstrumentationRule> resolve(InstrumentationSettings source, Map<String, ResolvedGenericDataProviderConfig> dataProviders) {
        if (CollectionUtils.isEmpty(source.getRules())) {
            return Collections.emptySet();
        }

        Map<String, InstrumentationScope> scopeMap = scopeResolver.resolve(source);

        Set<InstrumentationRule> rules = source.getRules()
                .entrySet()
                .stream()
                .filter(e -> e.getValue().isEnabled())
                .map(e -> resolveRule(e.getKey(), e.getValue(), scopeMap, dataProviders))
                .collect(Collectors.toSet());

        return rules;
    }

    /**
     * Creating the {@link InstrumentationRule} instance and linking the scopes as well as the data providers to it.
     */
    private InstrumentationRule resolveRule(String name, InstrumentationRuleSettings settings, Map<String, InstrumentationScope> scopeMap, Map<String, ResolvedGenericDataProviderConfig> dataProviders) {
        val result = InstrumentationRule.builder();
        result.name(name);
        settings.getScopes().entrySet()
                .stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .map(scopeMap::get)
                .filter(Objects::nonNull)
                .forEach(result::scope);

        settings.getEntry().forEach((data, call) ->
                result.entryProvider(data, resolveCall(call, dataProviders))
        );

        settings.getExit().forEach((data, call) ->
                result.exitProvider(data, resolveCall(call, dataProviders))
        );

        return result.build();
    }

    /**
     * Resolves a {@link DataProviderCallSettings} instance into a {@link ResolvedDataProviderCall}.
     * As this involves linking the {@link ResolvedGenericDataProviderConfig} of the provider which is used,
     * the map of known providers is required as input.
     *
     * @param dataProviders a map mapping the names of data providers to their resolved configuration.
     * @param call
     * @return
     */
    private ResolvedDataProviderCall resolveCall(DataProviderCallSettings call, Map<String, ResolvedGenericDataProviderConfig> dataProviders) {
        return ResolvedDataProviderCall.builder()
                .provider(dataProviders.get(call.getProvider()))
                .callSettings(call)
                .build();
    }
}
