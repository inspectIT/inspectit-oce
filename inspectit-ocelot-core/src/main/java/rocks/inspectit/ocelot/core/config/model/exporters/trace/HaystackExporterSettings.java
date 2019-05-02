package rocks.inspectit.ocelot.core.config.model.exporters.trace;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Data
@NoArgsConstructor
public class HaystackExporterSettings {

    private boolean enabled;

    /**
     * The hostname of the Haystack agent.
     */
    private String host;

    /**
     * The port of the Haystack agent.
     */
    @Min(0)
    @Max(65535)
    private int port;

    /**
     * The service name under which traces are published, defaults to inspectit.service-name;
     */
    private String serviceName;

}
