package rocks.inspectit.oce;

import io.opencensus.impl.internal.DisruptorEventQueue;
import io.opencensus.stats.AggregationData;
import io.opencensus.stats.Stats;
import io.opencensus.stats.View;
import io.opencensus.stats.ViewManager;
import io.opencensus.tags.InternalUtils;
import io.opencensus.tags.Tags;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class TestUtils {

    /**
     * OpenCensus internally manages a queue of events.
     * We simply add an event to the queue and wait until it is processed.
     */
    public static void waitForOpenCensusQueueToBeProcessed() {
        CountDownLatch latch = new CountDownLatch(1);
        DisruptorEventQueue.getInstance().enqueue(latch::countDown);
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void waitForInstrumentationToComplete() {
        await().atMost(30, TimeUnit.SECONDS).ignoreExceptions().untilAsserted(() -> {
            assertThat(getInstrumentationClassesCount()).isGreaterThan(0);
            assertThat(getInstrumentationQueueLength()).isZero();
            assertThat(getHookingQueueLength()).isZero();
            Thread.sleep(200); //to ensure that new-class-discovery has been executed
            assertThat(getInstrumentationQueueLength()).isZero();
            assertThat(getHookingQueueLength()).isZero();
        });

    }

    public static Map<String, String> getCurrentTagsAsMap() {
        Map<String, String> result = new HashMap<>();
        InternalUtils.getTags(Tags.getTagger().getCurrentTagContext())
                .forEachRemaining(t -> result.put(t.getKey().getName(), t.getValue().asString()));
        return result;
    }

    private static long getInstrumentationQueueLength() {
        ViewManager viewManager = Stats.getViewManager();
        AggregationData.LastValueDataLong queueSize =
                (AggregationData.LastValueDataLong)
                        viewManager.getView(View.Name.create("inspectit/self/instrumentation-analysis-queue-size"))
                                .getAggregationMap().values().stream()
                                .findFirst()
                                .get();
        return queueSize.getLastValue();
    }

    private static long getHookingQueueLength() {
        ViewManager viewManager = Stats.getViewManager();
        AggregationData.LastValueDataLong queueSize =
                (AggregationData.LastValueDataLong)
                        viewManager.getView(View.Name.create("inspectit/self/instrumentation-hooking-queue-size"))
                                .getAggregationMap().values().stream()
                                .findFirst()
                                .get();
        return queueSize.getLastValue();
    }

    private static long getInstrumentationClassesCount() {
        ViewManager viewManager = Stats.getViewManager();
        AggregationData.LastValueDataLong queueSize =
                (AggregationData.LastValueDataLong)
                        viewManager.getView(View.Name.create("inspectit/self/instrumented-classes"))
                                .getAggregationMap().values().stream()
                                .findFirst()
                                .get();
        return queueSize.getLastValue();
    }

}

