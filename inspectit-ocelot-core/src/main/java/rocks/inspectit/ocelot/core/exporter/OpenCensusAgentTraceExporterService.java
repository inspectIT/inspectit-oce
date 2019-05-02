package rocks.inspectit.ocelot.core.exporter;

import io.opencensus.common.Duration;
import io.opencensus.exporter.trace.ocagent.OcAgentTraceExporter;
import io.opencensus.exporter.trace.ocagent.OcAgentTraceExporterConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.config.model.InspectitConfig;
import rocks.inspectit.ocelot.core.config.model.exporters.trace.OpenCensusAgentTraceExporterSettings;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import javax.validation.Valid;

@Component
@Slf4j
public class OpenCensusAgentTraceExporterService extends DynamicallyActivatableService {

    public OpenCensusAgentTraceExporterService() {
        super("exporters.tracing.open-census-agent", "tracing.enabled");
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig conf) {
        @Valid OpenCensusAgentTraceExporterSettings openCensusAgent = conf.getExporters().getTracing().getOpenCensusAgent();
        return openCensusAgent.isEnabled();
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        try {
            OpenCensusAgentTraceExporterSettings settings = configuration.getExporters().getTracing().getOpenCensusAgent();
            log.info("Starting OpenCensus Agent Trace exporter");
            OcAgentTraceExporter.createAndRegister(OcAgentTraceExporterConfiguration.builder().setEndPoint(settings.getAddress()).setServiceName(settings.getServiceName()).setUseInsecure(settings.isUseInsecure()).setRetryInterval(Duration.create(settings.getReconnectionPeriod(), 0)).build());
            return true;
        } catch (Throwable t) {
            log.error("Error creating OpenCensus Agent Trace exporter", t);
            return false;
        }
    }

    @Override
    protected boolean doDisable() {
        log.info("Stopping OpenCensus Agent Trace exporter");
        try {
            OcAgentTraceExporter.unregister();
        } catch (Throwable t) {
            log.error("Error disabling OpenCensus Agent Trace exporter", t);
        }
        return true;
    }
}
