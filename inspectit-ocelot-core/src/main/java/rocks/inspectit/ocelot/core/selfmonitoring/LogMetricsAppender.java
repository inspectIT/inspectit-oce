package rocks.inspectit.ocelot.core.selfmonitoring;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import lombok.val;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;

/**
 * Logback appender which counts the amount of log events for all existing log levels.
 */
@Component
public class LogMetricsAppender extends UnsynchronizedAppenderBase<ILoggingEvent> implements SmartLifecycle {
    @Autowired
    SelfMonitoringService selfMonitoringService;

    @Override
    protected void append(ILoggingEvent event) {
        val customTags = new HashMap<String, String>();
        customTags.put("level", event.getLevel().levelStr);
        selfMonitoringService.recordMeasurement("logs",1l, customTags);
    }

    @Override
    public boolean isRunning() {
        return isStarted();
    }

    @PostConstruct
    private void appendToLogger() {
        Logger rootLogger = ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(this);
    }

}