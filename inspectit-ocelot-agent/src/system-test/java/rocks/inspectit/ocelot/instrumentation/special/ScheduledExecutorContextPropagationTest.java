package rocks.inspectit.ocelot.instrumentation.special;

import io.opencensus.common.Scope;
import io.opencensus.tags.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.instrumentation.InstrumentationSysTestBase;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class ScheduledExecutorContextPropagationTest extends InstrumentationSysTestBase {

    static class TestRunnable implements Runnable {

        private final Consumer<Void> callback;

        public TestRunnable(Consumer<Void> callback) {
            this.callback = callback;
        }

        @Override
        public void run() {
            callback.accept(null);
        }
    }

    static class TestCallable<T> implements Callable<T> {

        private final Function<Void, T> callback;

        public TestCallable(Function<Void, T> callback) {
            this.callback = callback;
        }

        @Override
        public T call() throws Exception {
            return callback.apply(null);
        }
    }

    private static final Tagger tagger = Tags.getTagger();

    private ScheduledExecutorService executorService;

    @BeforeAll
    public static void beforeAll() throws ClassNotFoundException {
        Executors.newSingleThreadScheduledExecutor().schedule(Math::random, 1, TimeUnit.MILLISECONDS);
        TestUtils.waitForClassInstrumentations(ScheduledThreadPoolExecutor.class);
    }

    @BeforeEach
    public void beforeEach() throws ExecutionException, InterruptedException {
        // warmup the executor - if this is not be done, the first call to the executor will always be correlated
        // because a thread is started, thus, it is correlated due to the Thread.start correlation
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.submit(Math::random).get();
    }

    @Nested
    public class Schedule_runnable {

        @Test
        public void verifyCtxPropagationViaScheduleRunnable_lambda() throws Exception {
            TagKey tagKey = TagKey.create("test-tag-key");
            TagValue tagValue = TagValue.create("test-tag-value");
            AtomicReference<Iterator<Tag>> refTags = new AtomicReference<>();

            Runnable runnable = () -> refTags.set(InternalUtils.getTags(tagger.getCurrentTagContext()));

            ScheduledFuture<?> schedule;
            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                schedule = executorService.schedule(runnable, 1, TimeUnit.MILLISECONDS);
            }
            schedule.get();

            assertThat(refTags.get()).hasSize(1)
                    .extracting("key", "value")
                    .contains(tuple(tagKey, tagValue));
        }

        @Test
        public void verifyCtxPropagationViaScheduleRunnable_anonymous() throws Exception {
            TagKey tagKey = TagKey.create("test-tag-key");
            TagValue tagValue = TagValue.create("test-tag-value");
            AtomicReference<Iterator<Tag>> refTags = new AtomicReference<>();

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    refTags.set(InternalUtils.getTags(tagger.getCurrentTagContext()));
                }
            };

            ScheduledFuture<?> schedule;
            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                schedule = executorService.schedule(runnable, 1, TimeUnit.MILLISECONDS);
            }
            schedule.get();

            assertThat(refTags.get()).hasSize(1)
                    .extracting("key", "value")
                    .contains(tuple(tagKey, tagValue));
        }

        @Test
        public void verifyCtxPropagationViaScheduleRunnable_named() throws Exception {
            TagKey tagKey = TagKey.create("test-tag-key");
            TagValue tagValue = TagValue.create("test-tag-value");
            AtomicReference<Iterator<Tag>> refTags = new AtomicReference<>();

            Runnable runnable = new TestRunnable(unused -> refTags.set(InternalUtils.getTags(tagger.getCurrentTagContext())));

            ScheduledFuture<?> schedule;
            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                schedule = executorService.schedule(runnable, 1, TimeUnit.MILLISECONDS);
            }
            schedule.get();

            assertThat(refTags.get()).hasSize(1)
                    .extracting("key", "value")
                    .contains(tuple(tagKey, tagValue));
        }
    }

    @Nested
    public class Schedule_callable {

        @Test
        public void verifyCtxPropagationViaScheduleCallable_lambda() throws Exception {
            TagKey tagKey = TagKey.create("test-tag-key");
            TagValue tagValue = TagValue.create("test-tag-value");

            Callable<Iterator<Tag>> callable = () -> InternalUtils.getTags(tagger.getCurrentTagContext());

            ScheduledFuture<Iterator<Tag>> future;
            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                future = executorService.schedule(callable, 1, TimeUnit.MILLISECONDS);
            }
            Iterator<Tag> result = future.get();

            assertThat(result).hasSize(1)
                    .extracting("key", "value")
                    .contains(tuple(tagKey, tagValue));
        }

        @Test
        public void verifyCtxPropagationViaScheduleCallable_anonymous() throws Exception {
            TagKey tagKey = TagKey.create("test-tag-key");
            TagValue tagValue = TagValue.create("test-tag-value");

            Callable<Iterator<Tag>> callable = new Callable<Iterator<Tag>>() {
                @Override
                public Iterator<Tag> call() throws Exception {
                    return InternalUtils.getTags(tagger.getCurrentTagContext());
                }
            };

            ScheduledFuture<Iterator<Tag>> future;
            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                future = executorService.schedule(callable, 1, TimeUnit.MILLISECONDS);
            }
            Iterator<Tag> result = future.get();

            assertThat(result).hasSize(1)
                    .extracting("key", "value")
                    .contains(tuple(tagKey, tagValue));
        }

        @Test
        public void verifyCtxPropagationViaScheduleCallable_named() throws Exception {
            TagKey tagKey = TagKey.create("test-tag-key");
            TagValue tagValue = TagValue.create("test-tag-value");

            Callable<Iterator<Tag>> callable = new TestCallable<>(unused -> InternalUtils.getTags(tagger.getCurrentTagContext()));

            ScheduledFuture<Iterator<Tag>> future;
            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                future = executorService.schedule(callable, 1, TimeUnit.MILLISECONDS);
            }
            Iterator<Tag> result = future.get();

            assertThat(result).hasSize(1)
                    .extracting("key", "value")
                    .contains(tuple(tagKey, tagValue));
        }
    }

    @Nested
    public class ScheduleWithFixedDelay {

        @Test
        public void verifyCtxPropagationViaScheduleFixedDelay_lambda() throws Exception {
            int iterations = 5;
            TagKey tagKey = TagKey.create("test-tag-key");
            TagValue tagValue = TagValue.create("test-tag-value");
            CountDownLatch interationCount = new CountDownLatch(iterations);

            List<Iterator<Tag>> iteratorList = new CopyOnWriteArrayList<>();

            Runnable runnable = () -> {
                Iterator<Tag> iter = InternalUtils.getTags(tagger.getCurrentTagContext());
                iteratorList.add(iter);
                interationCount.countDown();
            };

            ScheduledFuture future;
            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                future = executorService.scheduleWithFixedDelay(runnable, 0, 1, TimeUnit.MILLISECONDS);
            }

            interationCount.await();

            future.cancel(true);
            executorService.shutdown();

            assertThat(iteratorList).size().isGreaterThanOrEqualTo(iterations);
            iteratorList.forEach(tagIterator -> assertThat(tagIterator)
                    .hasSize(1)
                    .extracting("key", "value")
                    .contains(tuple(tagKey, tagValue)));
        }

        @Test
        public void verifyCtxPropagationViaScheduleFixedDelay_anonymous() throws Exception {
            int iterations = 5;
            TagKey tagKey = TagKey.create("test-tag-key");
            TagValue tagValue = TagValue.create("test-tag-value");
            CountDownLatch interationCount = new CountDownLatch(iterations);

            List<Iterator<Tag>> iteratorList = new CopyOnWriteArrayList<>();

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    Iterator<Tag> iter = InternalUtils.getTags(tagger.getCurrentTagContext());
                    iteratorList.add(iter);
                    interationCount.countDown();
                }
            };

            ScheduledFuture future;
            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                future = executorService.scheduleWithFixedDelay(runnable, 0, 1, TimeUnit.MILLISECONDS);
            }

            interationCount.await();

            future.cancel(true);
            executorService.shutdown();

            assertThat(iteratorList).size().isGreaterThanOrEqualTo(iterations);
            iteratorList.forEach(tagIterator -> assertThat(tagIterator)
                    .hasSize(1)
                    .extracting("key", "value")
                    .contains(tuple(tagKey, tagValue)));
        }

        @Test
        public void verifyCtxPropagationViaScheduleFixedDelay_named() throws Exception {
            int iterations = 5;
            TagKey tagKey = TagKey.create("test-tag-key");
            TagValue tagValue = TagValue.create("test-tag-value");
            CountDownLatch interationCount = new CountDownLatch(iterations);

            List<Iterator<Tag>> iteratorList = new CopyOnWriteArrayList<>();

            Runnable runnable = new TestRunnable(unused -> {
                Iterator<Tag> iter = InternalUtils.getTags(tagger.getCurrentTagContext());
                iteratorList.add(iter);
                interationCount.countDown();
            });

            ScheduledFuture future;
            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                future = executorService.scheduleWithFixedDelay(runnable, 0, 1, TimeUnit.MILLISECONDS);
            }

            interationCount.await();

            future.cancel(true);
            executorService.shutdown();

            assertThat(iteratorList).size().isGreaterThanOrEqualTo(iterations);
            iteratorList.forEach(tagIterator -> assertThat(tagIterator)
                    .hasSize(1)
                    .extracting("key", "value")
                    .contains(tuple(tagKey, tagValue)));
        }
    }

    @Nested
    public class ScheduleAtFixedRate {

        @Test
        public void verifyCtxPropagationViaScheduleFixedRate_lambda() throws Exception {
            int iterations = 5;
            TagKey tagKey = TagKey.create("test-tag-key");
            TagValue tagValue = TagValue.create("test-tag-value");
            CountDownLatch interationCount = new CountDownLatch(iterations);

            List<Iterator<Tag>> iteratorList = new CopyOnWriteArrayList<>();

            Runnable runnable = () -> {
                Iterator<Tag> iter = InternalUtils.getTags(tagger.getCurrentTagContext());
                iteratorList.add(iter);
                interationCount.countDown();
            };

            ScheduledFuture future;
            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                future = executorService.scheduleAtFixedRate(runnable, 0, 1, TimeUnit.MILLISECONDS);
            }

            interationCount.await();

            future.cancel(true);
            executorService.shutdown();

            assertThat(iteratorList).size().isGreaterThanOrEqualTo(iterations);
            iteratorList.forEach(tagIterator -> assertThat(tagIterator)
                    .hasSize(1)
                    .extracting("key", "value")
                    .contains(tuple(tagKey, tagValue)));
        }

        @Test
        public void verifyCtxPropagationViaScheduleFixedRate_anonymous() throws Exception {
            int iterations = 5;
            TagKey tagKey = TagKey.create("test-tag-key");
            TagValue tagValue = TagValue.create("test-tag-value");
            CountDownLatch interationCount = new CountDownLatch(iterations);

            List<Iterator<Tag>> iteratorList = new CopyOnWriteArrayList<>();

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    Iterator<Tag> iter = InternalUtils.getTags(tagger.getCurrentTagContext());
                    iteratorList.add(iter);
                    interationCount.countDown();
                }
            };

            ScheduledFuture future;
            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                future = executorService.scheduleAtFixedRate(runnable, 0, 1, TimeUnit.MILLISECONDS);
            }

            interationCount.await();

            future.cancel(true);
            executorService.shutdown();

            assertThat(iteratorList).size().isGreaterThanOrEqualTo(iterations);
            iteratorList.forEach(tagIterator -> assertThat(tagIterator)
                    .hasSize(1)
                    .extracting("key", "value")
                    .contains(tuple(tagKey, tagValue)));
        }

        @Test
        public void verifyCtxPropagationViaScheduleFixedRate_named() throws Exception {
            int iterations = 5;
            TagKey tagKey = TagKey.create("test-tag-key");
            TagValue tagValue = TagValue.create("test-tag-value");
            CountDownLatch interationCount = new CountDownLatch(iterations);

            List<Iterator<Tag>> iteratorList = new CopyOnWriteArrayList<>();

            Runnable runnable = new TestRunnable(unused -> {
                Iterator<Tag> iter = InternalUtils.getTags(tagger.getCurrentTagContext());
                iteratorList.add(iter);
                interationCount.countDown();
            });

            ScheduledFuture future;
            try (Scope s = tagger.currentBuilder().putLocal(tagKey, tagValue).buildScoped()) {
                future = executorService.scheduleAtFixedRate(runnable, 0, 1, TimeUnit.MILLISECONDS);
            }

            interationCount.await();

            future.cancel(true);
            executorService.shutdown();

            assertThat(iteratorList).size().isGreaterThanOrEqualTo(iterations);
            iteratorList.forEach(tagIterator -> assertThat(tagIterator)
                    .hasSize(1)
                    .extracting("key", "value")
                    .contains(tuple(tagKey, tagValue)));
        }
    }
}
