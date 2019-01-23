package rocks.inspectit.oce.core.metrics;

import io.opencensus.stats.*;
import io.opencensus.tags.TagKey;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import rocks.inspectit.oce.core.config.model.InspectitConfig;
import rocks.inspectit.oce.core.config.model.metrics.MetricsSettings;
import rocks.inspectit.oce.core.service.DynamicallyActivatableService;
import rocks.inspectit.oce.core.tags.CommonTagsManager;

import java.util.*;
import java.util.function.Supplier;

/**
 * Base class for dynamically enableable metrics recorders.
 */
@Slf4j
public abstract class AbstractMetricsRecorder extends DynamicallyActivatableService {

    @Autowired
    protected StatsRecorder recorder;

    @Autowired
    protected ViewManager viewManager;

    @Autowired
    protected CommonTagsManager commonTags;

    /**
     * Storage for lazily created long measures.
     */
    private Map<String, Measure.MeasureLong> createdMeasureLongs = new HashMap<>();

    /**
     * Storage for lazily created double measures.
     */
    private Map<String, Measure.MeasureDouble> createdMeasureDoubles = new HashMap<>();

    /**
     * Constructor.
     * This class already handles the dependency to the master switch.
     *
     * @param configDependency the configuration dependency of the recorder.
     */
    public AbstractMetricsRecorder(String configDependency) {
        super("metrics.enabled", configDependency);
    }

    @Override
    protected final boolean checkEnabledForConfig(InspectitConfig conf) {
        MetricsSettings ms = conf.getMetrics();
        return ms.isEnabled() && checkEnabledForConfig(ms);
    }

    /**
     * Checks if this recorder is active for the given configuration.
     * The master switch does not need to be checked, this is handled by {@link AbstractMetricsRecorder}.
     *
     * @param ms the metrics settings
     * @return true if the recorder should be enabled
     */
    protected abstract boolean checkEnabledForConfig(MetricsSettings ms);

    /**
     * Lazily creates the given measure and a view with all common tags, if the measure was not created yet.
     *
     * @param name        the name of the measure and view
     * @param description the description of the measure and view
     * @param unit        the unit of the measure
     * @param aggregation the aggregation of the view
     * @return
     */
    protected Measure.MeasureLong getOrCreateMeasureLongWithView(String name, String description, String unit, Supplier<Aggregation> aggregation, TagKey... additionalTags) {
        return createdMeasureLongs.computeIfAbsent(name, (n) -> {
            val measure = Measure.MeasureLong.create(name, description, unit);
            List<TagKey> allTags = new ArrayList<>(commonTags.getCommonTagKeys());
            allTags.addAll(Arrays.asList(additionalTags));
            View.Name viewName = View.Name.create(measure.getName());
            if (viewManager.getView(viewName) == null) {
                val view = View.create(viewName, measure.getDescription() + " [" + measure.getUnit() + "]",
                        measure, aggregation.get(), allTags);
                viewManager.registerView(view);
            } else {
                log.info("View with the name {} is already existent in OpenCensus, no new view is registered", name);
            }
            return measure;
        });
    }

    /**
     * Lazily creates the given measure and a view with all common tags, if the measure was not created yet.
     *
     * @param name        the name of the measure and view
     * @param description the description of the measure and view
     * @param unit        the unit of the measure
     * @param aggregation the aggregation of the view
     * @return
     */
    protected Measure.MeasureDouble getOrCreateMeasureDoubleWithView(String name, String description, String unit, Supplier<Aggregation> aggregation, TagKey... additionalTags) {
        return createdMeasureDoubles.computeIfAbsent(name, (n) -> {
            val measure = Measure.MeasureDouble.create(name, description, unit);
            List<TagKey> allTags = new ArrayList<>(commonTags.getCommonTagKeys());
            allTags.addAll(Arrays.asList(additionalTags));
            View.Name viewName = View.Name.create(measure.getName());
            if (viewManager.getView(viewName) == null) {
                val view = View.create(viewName, measure.getDescription() + " [" + measure.getUnit() + "]",
                        measure, aggregation.get(), allTags);
                viewManager.registerView(view);
            } else {
                log.info("View with the name {} is already existent in OpenCensus, no new view is registered", name);
            }
            return measure;
        });
    }

}
