package rocks.inspectit.oce.core.config.model.instrumentation;

import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.oce.core.config.model.InspectitConfig;
import rocks.inspectit.oce.core.config.model.instrumentation.data.DataSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.dataproviders.GenericDataProviderSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.rules.InstrumentationRuleSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.scope.InstrumentationScopeSettings;
import rocks.inspectit.oce.core.config.model.validation.ViolationBuilder;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Configuration object for all settings regarding the instrumentation.
 */
@Data
@NoArgsConstructor
public class InstrumentationSettings {

    /**
     * The configuration of internal parameters regarding the instrumentation process.
     */
    @Valid
    @NotNull
    private InternalSettings internal;

    /**
     * The configuration for all special sensors.
     */
    @Valid
    @NotNull
    private SpecialSensorSettings special;

    /**
     * Defines which packages of the bootstrap should not be instrumented.
     * All classes from the given packages and their subpackages will be ignored.
     */
    @NotNull
    private Map<@NotBlank String, Boolean> ignoredBootstrapPackages = Collections.emptyMap();

    /**
     * Defines which packages of all class loaders should not be instrumented.
     * All classes from the given packages and their subpackages will be ignored.
     */
    @NotNull
    private Map<@NotBlank String, Boolean> ignoredPackages = Collections.emptyMap();

    /**
     * All defined custom data providers, the key defines their name.
     * The name is case sensitive!
     */
    @NotNull
    private Map<@NotBlank String, @Valid GenericDataProviderSettings> dataProviders = Collections.emptyMap();


    /**
     * The configuration of the defined scopes. The map's key represents an unique id for the related instrumentation scope.
     */
    @NotNull
    private Map<@NotBlank String, @Valid InstrumentationScopeSettings> scopes = Collections.emptyMap();

    /**
     * The configuration of the defined rules. The map's key represents an unique id for the related instrumentation rule.
     */
    @NotNull
    private Map<@NotBlank String, @Valid InstrumentationRuleSettings> rules = Collections.emptyMap();

    /**
     * Defines the behaviour of the data regarding context propagation.
     * E.g. is data propagated up and/or down, is it visible as a Tag for metrics collection?
     */
    @NotNull
    private Map<@NotBlank String, @Valid DataSettings> data = Collections.emptyMap();

    public void performValidation(InspectitConfig container, ViolationBuilder vios) {
        Set<String> declaredMetrics = container.getMetrics().getDefinitions().keySet();
        rules.forEach((name, r) ->
                r.performValidation(this, declaredMetrics, vios.atProperty("rules").atProperty(name)));
    }

}
