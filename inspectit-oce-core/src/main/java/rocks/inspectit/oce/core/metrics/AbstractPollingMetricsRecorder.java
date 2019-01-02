package rocks.inspectit.oce.core.metrics;

import io.opencensus.stats.MeasureMap;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import rocks.inspectit.oce.core.config.model.InspectitConfig;
import rocks.inspectit.oce.core.config.model.metrics.MetricsSettings;
import rocks.inspectit.oce.core.selfmonitoring.SelfMonitoringService;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Base class for all metrics recorders which perform polling to acquire the measurement data.
 */
@Slf4j
public abstract class AbstractPollingMetricsRecorder extends AbstractMetricsRecorder {

    @Autowired
    protected ScheduledExecutorService executor;

    @Autowired
    protected SelfMonitoringService selfMonitoringService;

    private ScheduledFuture<?> pollingTask;

    public AbstractPollingMetricsRecorder(String configDependency) {
        super(configDependency);
    }

    /**
     * Called to take a measurement by {@link #takeMeasurement(MetricsSettings, MeasureMap)}.
     * This method is invoked with the frequency returned by {@link #getFrequency(MetricsSettings)} when enabled.
     * The {@link MeasureMap#record()} method invocation is handled by {@link AbstractPollingMetricsRecorder}!
     * This method is called within a common tag scope, meaning that tags to additional MeasureMaps are automatically added
     *
     * @param config      the current configuration
     * @param measurement the {@link MeasureMap} to store the measurements in
     */
    protected void takeMeasurement(MetricsSettings config, MeasureMap measurement) {
    }

    /**
     * Called to take a measurement.
     * This method is invoked with the frequency returned by {@link #getFrequency(MetricsSettings)} when enabled.
     * This method should be overwritten when custom tags are required for the MeasurementMap.
     * The default implementation simply invokes {@link #takeMeasurement(MetricsSettings, MeasureMap)}.
     *
     * @param config the current configuration
     */
    protected void takeMeasurement(MetricsSettings config) {
        val mm = recorder.newMeasureMap();
        takeMeasurement(config, mm);
        mm.record();
    }

    /**
     * Extracts the polling frequency from the given metrics configuration.
     *
     * @param config the configuration to extract from
     * @return the frequency
     */
    protected abstract Duration getFrequency(MetricsSettings config);

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        log.info("Enabling {}.", getClass().getSimpleName());
        val conf = configuration.getMetrics();
        pollingTask = executor.scheduleWithFixedDelay(() -> {
            try (val scope = selfMonitoringService.withSelfMonitoring(getClass().getSimpleName())) {
                try (val tags = commonTags.withCommonTagScope()) {
                    takeMeasurement(conf);
                }
            } catch (Exception e) {
                log.error("Error taking measurement", e);
            }
        }, 0, getFrequency(conf).toMillis(), TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    protected boolean doDisable() {
        log.info("Disabling {}.", getClass().getSimpleName());
        pollingTask.cancel(true);
        return true;
    }

}
