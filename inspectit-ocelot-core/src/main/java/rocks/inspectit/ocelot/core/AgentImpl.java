package rocks.inspectit.ocelot.core;

import io.opencensus.tags.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import rocks.inspectit.ocelot.bootstrap.IAgent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.config.spring.SpringConfiguration;
import rocks.inspectit.ocelot.core.logging.logback.LogbackInitializer;

import java.lang.instrument.Instrumentation;
import java.util.Optional;

/**
 * Implementation for the {@link IAgent} interface.
 * This class is responsible for setting up the spring context for inspectIT.
 *
 * @author Jonas Kunz
 */
public class AgentImpl implements IAgent {

    // statically initialize our default logging before doing anything
    static {
        LogbackInitializer.initDefaultLogging();
        LOGGER = LoggerFactory.getLogger(AgentImpl.class);
    }

    /**
     * Logger that is initialized in the static init block
     */
    private static final Logger LOGGER;

    /**
     * Created application context.
     */
    private AnnotationConfigApplicationContext ctx;

    @Override
    public void start(String cmdArgs, Instrumentation instrumentation) {
        ClassLoader classloader = AgentImpl.class.getClassLoader();

        LOGGER.info("Starting inspectIT Ocelot Agent...");
        logOpenCensusClassLoader();

        ctx = new AnnotationConfigApplicationContext();
        ctx.setClassLoader(classloader);
        InspectitEnvironment environment = new InspectitEnvironment(ctx, Optional.ofNullable(cmdArgs));

        // once we have the environment, init the logging with the config
        LogbackInitializer.initLogging(environment.getCurrentConfig());

        ctx.registerShutdownHook();

        //Allows to use autowiring to acquire the Instrumentation instance
        ctx.addBeanFactoryPostProcessor(bf -> bf.registerSingleton("instrumentation", instrumentation));

        ctx.register(SpringConfiguration.class);
        ctx.refresh();
    }

    private void logOpenCensusClassLoader() {
        if (Tags.class.getClassLoader() == AgentImpl.class.getClassLoader()) {
            LOGGER.info("OpenCensus was loaded in inspectIT classloader");
        } else {
            LOGGER.info("OpenCensus was loaded in bootstrap classloader");
        }
    }


    @Override
    public void destroy() {
        LOGGER.info("Shutting down inspectIT Ocelot Agent");
        ctx.close();
    }
}
