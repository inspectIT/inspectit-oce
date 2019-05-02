package rocks.inspectit.ocelot.core.exporter;

import io.opencensus.common.Duration;
import io.opencensus.exporter.stats.signalfx.SignalFxStatsConfiguration;
import io.opencensus.exporter.stats.signalfx.SignalFxStatsExporter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.config.model.InspectitConfig;
import rocks.inspectit.ocelot.core.config.model.exporters.metrics.SignalFxExporterSettings;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import javax.validation.Valid;

@Component
@Slf4j
public class SignalFxExporterService extends DynamicallyActivatableService {

    public SignalFxExporterService() {
        super("exporters.metrics.signal-fx", "metrics.enabled");
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig conf) {
        @Valid SignalFxExporterSettings signalFx = conf.getExporters().getMetrics().getSignalFx();
        return signalFx.isEnabled();
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        try {
            SignalFxExporterSettings settings = configuration.getExporters().getMetrics().getSignalFx();
            log.info("Starting SignalFx exporter");
            SignalFxStatsExporter.create(SignalFxStatsConfiguration.builder()
                    .setToken(settings.getToken())
                    .setExportInterval(Duration.create(settings.getReportingInterval(), 0))
                    .build());
            return true;
        } catch (Throwable t) {
            log.error("Error creating SignalFx exporter", t);
            return false;
        }
    }

    @Override
    protected boolean doDisable() {
        log.info("The SignalFx exporter cannot be stopped during runtime. In order to disable the exporter, please reattach the agent.");
        return true;
    }
}
