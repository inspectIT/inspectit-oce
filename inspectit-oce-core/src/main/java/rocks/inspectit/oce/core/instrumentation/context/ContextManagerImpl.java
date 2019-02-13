package rocks.inspectit.oce.core.instrumentation.context;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.grpc.Context;
import lombok.val;
import rocks.inspectit.oce.bootstrap.context.ContextManager;
import rocks.inspectit.oce.core.config.spring.BootstrapInitializerConfiguration;
import rocks.inspectit.oce.core.instrumentation.config.InstrumentationConfigurationResolver;
import rocks.inspectit.oce.core.tags.CommonTagsManager;

import java.util.concurrent.Callable;

/**
 * This class is based on the ContextStrategyImpl (https://github.com/census-instrumentation/opencensus-java/blob/master/contrib/agent/src/main/java/io/opencensus/contrib/agent/instrumentation/ContextStrategyImpl.java)
 * class from the opencensus-java repository.
 */
public class ContextManagerImpl implements ContextManager {

    /**
     * The name of this singleton injected by {@link BootstrapInitializerConfiguration}.
     * Can be used in {@link org.springframework.context.annotation.DependsOn} annotation to ensure correct initialization order.
     */
    public static final String BEAN_NAME = "contextManager";

    private CommonTagsManager commonTags;

    private InstrumentationConfigurationResolver configProvider;

    /**
     * Cache for storing the context objects.
     */
    private final Cache<Thread, Context> storedContexts = CacheBuilder.newBuilder().weakKeys().build();

    public ContextManagerImpl(CommonTagsManager commonTags, InstrumentationConfigurationResolver configProvider) {
        this.commonTags = commonTags;
        this.configProvider = configProvider;
    }

    @Override
    public Runnable wrap(Runnable r) {
        Runnable result;
        try (val upPropagationBarrier = enterNewContext()) {
            result = Context.current().wrap(r);
        }
        return result;
    }

    @Override
    public <T> Callable<T> wrap(Callable<T> callable) {
        Callable<T> result;
        try (val upPropagationBarrier = enterNewContext()) {
            result = Context.current().wrap(callable);
        }
        return result;
    }

    @Override
    public void storeContextForThread(Thread thread) {
        try (val upPropagationBarrier = enterNewContext()) {
            storedContexts.put(thread, Context.current());
        }
    }

    @Override
    public void attachContextToThread(Thread thread) {
        Context context = storedContexts.getIfPresent(thread);
        if (context != null) {
            storedContexts.invalidate(thread);
            context.attach();
        }
    }

    public InspectitContext enterNewContext() {
        return InspectitContext.createAndEnter(commonTags, configProvider.getCurrentConfig().getDataProperties());
    }
}
