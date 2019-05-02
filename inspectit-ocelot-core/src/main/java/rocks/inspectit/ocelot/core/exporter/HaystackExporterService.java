package rocks.inspectit.ocelot.core.exporter;

import com.expedia.www.opencensus.exporter.trace.HaystackTraceExporter;
import com.expedia.www.opencensus.exporter.trace.config.GrpcAgentDispatcherConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.config.model.InspectitConfig;
import rocks.inspectit.ocelot.core.config.model.exporters.trace.HaystackExporterSettings;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import javax.validation.Valid;

@Component
@Slf4j
public class HaystackExporterService extends DynamicallyActivatableService {

    public HaystackExporterService() {
        super("exporters.tracing.haystack", "tracing.enabled");
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig conf) {
        @Valid HaystackExporterSettings haystack = conf.getExporters().getTracing().getHaystack();
        return haystack.isEnabled();
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        try {
            HaystackExporterSettings settings = configuration.getExporters().getTracing().getHaystack();
            log.info("Starting Haystack exporter");
            HaystackTraceExporter.createAndRegister(new GrpcAgentDispatcherConfig(settings.getHost(), settings.getPort()), settings.getServiceName());
            return true;
        } catch (Throwable t) {
            log.error("Error creating Haystack exporter", t);
            return false;
        }
    }

    @Override
    protected boolean doDisable() {
        log.info("Stopping Haystack exporter");
        try {
           HaystackTraceExporter.unregister();
        } catch (Throwable t) {
            log.error("Error disabling Haystack exporter", t);
        }
        return true;
    }
}
