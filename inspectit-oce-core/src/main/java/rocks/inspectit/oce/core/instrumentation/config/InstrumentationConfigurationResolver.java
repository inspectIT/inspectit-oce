package rocks.inspectit.oce.core.instrumentation.config;

import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import rocks.inspectit.oce.bootstrap.instrumentation.DoNotInstrumentMarker;
import rocks.inspectit.oce.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.oce.core.config.InspectitEnvironment;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.instrumentation.AsyncClassTransformer;
import rocks.inspectit.oce.core.instrumentation.config.event.InstrumentationConfigurationChangedEvent;
import rocks.inspectit.oce.core.instrumentation.config.model.*;
import rocks.inspectit.oce.core.instrumentation.special.SpecialSensor;
import rocks.inspectit.oce.core.utils.CommonUtils;

import javax.annotation.PostConstruct;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is responsible for deriving the {@link InstrumentationConfiguration} from
 * the {@link InstrumentationSettings}.
 */
@Service
@Slf4j
public class InstrumentationConfigurationResolver {

    private static final ClassLoader INSPECTIT_CLASSLOADER = AsyncClassTransformer.class.getClassLoader();

    @Autowired
    private InspectitEnvironment env;

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private Instrumentation instrumentation;

    @Autowired
    private List<SpecialSensor> specialSensors;

    @Autowired
    private InstrumentationRuleResolver ruleResolver;

    @Autowired
    private DataProviderResolver dataProviderResolver;

    @Autowired
    private MethodHookConfigurationResolver hookResolver;


    /**
     * Holds the currently active instrumentation configuration.
     */
    @Getter
    private InstrumentationConfiguration currentConfig;

    @PostConstruct
    private void init() {
        currentConfig = resolveConfiguration(env.getCurrentConfig().getInstrumentation());
    }

    /**
     * Builds the {@link ClassInstrumentationConfiguration} based on the currently active global instrumentation configuration
     * for the given class.
     *
     * @param clazz the class for which the configuration shal lbe queried
     * @return the configuration or {@link ClassInstrumentationConfiguration#NO_INSTRUMENTATION} if this class should not be instrumented
     */
    public ClassInstrumentationConfiguration getClassInstrumentationConfiguration(Class<?> clazz) {
        val config = currentConfig;
        try {
            if (isIgnoredClass(clazz, config)) {
                return ClassInstrumentationConfiguration.NO_INSTRUMENTATION;

            } else {
                TypeDescription description = TypeDescription.ForLoadedType.of(clazz);
                Set<SpecialSensor> activeSensors = specialSensors.stream()
                        .filter(s -> s.shouldInstrument(description, config))
                        .collect(Collectors.toSet());

                Set<InstrumentationRule> narrowedRules = getNarrowedRulesFor(description, config);

                return new ClassInstrumentationConfiguration(activeSensors, narrowedRules, config);

            }
        } catch (NoClassDefFoundError e) {
            //the class contains a reference to an not loadable class
            //this the case for example for very many spring boot classes
            log.trace("Ignoring class {} for instrumentation as it is not initializable ", clazz.getName(), e);
            return ClassInstrumentationConfiguration.NO_INSTRUMENTATION;
        }
    }

    /**
     * Finds out for each method of the given class which rules apply and builds a {@link MethodHookConfiguration} for each instrumented method.
     *
     * @param clazz the class to check
     * @return a map mapping hook configurations to the methods which they should be applied on.
     */
    public Map<MethodDescription, MethodHookConfiguration> getHookConfigurations(Class<?> clazz) {
        val config = currentConfig;
        try {
            TypeDescription type = TypeDescription.ForLoadedType.of(clazz);
            Set<InstrumentationRule> narrowedRules = getNarrowedRulesFor(type, config);

            Set<InstrumentationScope> involvedScopes = narrowedRules.stream()
                    .flatMap(r -> r.getScopes().stream())
                    .collect(Collectors.toSet());

            if (!narrowedRules.isEmpty()) {
                Map<MethodDescription, MethodHookConfiguration> result = new HashMap<>();
                for (val method : type.getDeclaredMethods()) {
                    val rulesMatchingOnMethod = narrowedRules.stream()
                            .filter(rule -> rule.getScopes().stream()
                                    .anyMatch(scope -> scope.getMethodMatcher().matches(method)))
                            .collect(Collectors.toSet());
                    if (!rulesMatchingOnMethod.isEmpty()) {
                        try {
                            result.put(method, hookResolver.buildHookConfiguration(clazz, method, rulesMatchingOnMethod));
                        } catch (MethodHookConfigurationResolver.CyclicDataDependencyException e) {
                            log.error("Could not build hook for {} of class {} due to cyclic dependency between data assignments: {}",
                                    CommonUtils.getSignature(method), clazz.getName(), e.getDependencyCycle().toString());
                        } catch (MethodHookConfigurationResolver.ConflictingDataDefinitionsException e) {
                            log.error("Could not build hook for {} of class {} due to conflicting data assignments for data {} of rule {} and rule {}.",
                                    CommonUtils.getSignature(method), clazz.getName(), e.getDataKey(), e.getFirst().getName(), e.getSecond().getName());
                        }
                    }
                }
                return result;
            }
        } catch (NoClassDefFoundError e) {
            //the class contains a reference to an not loadable class
            //this the case for example for very many spring boot classes
            log.trace("Ignoring class {} for hooking as it is not initializable ", clazz.getName(), e);
        }
        return Collections.emptyMap();


    }

    /**
     * Narrows a rule for a specific type. The rules existing in the returned set are containing only {@link rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationScope}s
     * which are matching for the given type. This prevents that method matchers will be applied to the wrong types.
     *
     * @param typeDescription the class which are the rules targeting
     * @param config          the configuration which is used as basis for the rules
     * @return Returns a set containing rules with scopes targeting only the given type.
     */
    private Set<InstrumentationRule> getNarrowedRulesFor(TypeDescription typeDescription, InstrumentationConfiguration config) {
        return config.getRules().stream()
                .map(rule -> Pair.of(
                        rule,
                        rule.getScopes()
                                .stream()
                                .filter(s -> s.getTypeMatcher().matches(typeDescription))
                                .collect(Collectors.toSet())))
                .filter(p -> !p.getRight().isEmpty())
                .map(p -> p.getLeft().toBuilder().clearScopes().scopes(p.getRight()).build())
                .collect(Collectors.toSet());
    }

    @EventListener
    private void inspectitConfigChanged(InspectitConfigChangedEvent ev) {

        InstrumentationSettings oldSettings = ev.getOldConfig().getInstrumentation();
        InstrumentationSettings newSettings = ev.getNewConfig().getInstrumentation();

        if (!Objects.equals(oldSettings, newSettings)) {
            val oldConfig = currentConfig;
            val newConfig = resolveConfiguration(ev.getNewConfig().getInstrumentation());
            if (!Objects.equals(oldConfig, newConfig)) {
                currentConfig = newConfig;
                val event = new InstrumentationConfigurationChangedEvent(this, oldConfig, currentConfig);
                ctx.publishEvent(event);
            }
        }
    }

    private InstrumentationConfiguration resolveConfiguration(InstrumentationSettings source) {
        val dataProviders = dataProviderResolver.resolveProviders(source);
        return InstrumentationConfiguration.builder()
                .source(source)
                .rules(ruleResolver.resolve(source, dataProviders))
                .dataProperties(resolveDataProperties(source))
                .build();
    }

    @VisibleForTesting
    DataProperties resolveDataProperties(InstrumentationSettings source) {
        val builder = DataProperties.builder();
        source.getData().forEach(builder::data);
        return builder.build();
    }


    /**
     * Checks if the given class should not be instrumented based on the given configuration.
     *
     * @param clazz  the class to check
     * @param config configuration to check for
     * @return true, if the class is ignored (=it should not be instrumented)
     */
    @VisibleForTesting
    boolean isIgnoredClass(Class<?> clazz, InstrumentationConfiguration config) {

        if (!instrumentation.isModifiableClass(clazz)) {
            return true;
        }

        if (DoNotInstrumentMarker.class.isAssignableFrom(clazz)) {
            return true;
        }

        if (clazz.getClassLoader() == INSPECTIT_CLASSLOADER) {
            return true;
        }

        if (isLambdaWithDefaultMethod(clazz)) {
            return true;
        }

        String name = clazz.getName();

        boolean isIgnored = config.getSource().getIgnoredPackages().entrySet().stream()
                .filter(Map.Entry::getValue)
                .anyMatch(e -> name.startsWith(e.getKey()));
        if (isIgnored) {
            return true;
        }

        if (clazz.getClassLoader() == null) {
            boolean isIgnoredOnBootstrap = config.getSource().getIgnoredBootstrapPackages().entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .anyMatch(e -> name.startsWith(e.getKey()));
            if (isIgnoredOnBootstrap) {
                return true;
            }
        }
        return false;
    }

    private boolean isLambdaWithDefaultMethod(Class<?> clazz) {
        return clazz.getName().contains("/") && Stream.of(clazz.getMethods()).anyMatch(Method::isDefault);
    }
}
