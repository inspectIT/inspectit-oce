package rocks.inspectit.ocelot.core.exporter;

import io.opencensus.common.Duration;
import io.opencensus.exporter.metrics.ocagent.OcAgentMetricsExporter;
import io.opencensus.exporter.metrics.ocagent.OcAgentMetricsExporterConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.config.model.InspectitConfig;
import rocks.inspectit.ocelot.core.config.model.exporters.metrics.OpenCensusAgentMetricsExporterSettings;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import javax.validation.Valid;

@Component
@Slf4j
public class OpenCensusAgentMetricsExporterService extends DynamicallyActivatableService {

    public OpenCensusAgentMetricsExporterService() {
        super("exporters.metrics.open-census-agent", "metrics.enabled");
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig conf) {
        @Valid OpenCensusAgentMetricsExporterSettings openCensusAgent = conf.getExporters().getMetrics().getOpenCensusAgent();
        return openCensusAgent.isEnabled();
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        try {
            OpenCensusAgentMetricsExporterSettings settings = configuration.getExporters().getMetrics().getOpenCensusAgent();
            log.info("Starting OpenCensus Agent Metrics exporter");
            OcAgentMetricsExporter.createAndRegister(OcAgentMetricsExporterConfiguration.builder().setExportInterval(Duration.create(settings.getExportInterval(), 0)).setEndPoint(settings.getAddress()).setServiceName(settings.getServiceName()).setUseInsecure(settings.isUseInsecure()).setRetryInterval(Duration.create(settings.getReconnectionPeriod(), 0)).build());
            return true;
        } catch (Throwable t) {
            log.error("Error creating OpenCensus Agent Metrics exporter", t);
            return false;
        }
    }

    @Override
    protected boolean doDisable() {
        log.info("The OpenCensus Agent Metrics exporter cannot be stopped during runtime. In order to disable the exporter, please reattach the agent.");
        return true;
    }
}
