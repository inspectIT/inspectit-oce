package rocks.inspectit.ocelot.core.config.model.exporters.metrics;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SignalFxExporterSettings {

    private boolean enabled;

    /**
     * The SignalFx token.
     */
    private String token;

    /**
     * The reporting interval in seconds.
     */
    private int reportingInterval;
}
