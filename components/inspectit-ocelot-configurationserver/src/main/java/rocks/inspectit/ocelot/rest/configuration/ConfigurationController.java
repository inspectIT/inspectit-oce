package rocks.inspectit.ocelot.rest.configuration;

import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import rocks.inspectit.ocelot.file.FileChangedEvent;
import rocks.inspectit.ocelot.rest.AbstractBaseController;

@RestController
public class ConfigurationController extends AbstractBaseController {

    /**
     * Event publisher to trigger events upon incoming reload requests.
     */
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * Fires a new {@link FileChangedEvent}.
     */
    private void fireFileChangeEvent() {
        FileChangedEvent event = new FileChangedEvent(this);
        eventPublisher.publishEvent(event);
    }

    /**
     * Reloads all configuration files present in the servers working directory.
     */
    @ApiOperation(value = "Reloads all configuration files.", notes = "Reloads all configuration files present in the " +
            "servers working directory.")
    @GetMapping(value = "configuration/reload", produces = "text/plain")
    public void reloadConfiguration() {
        fireFileChangeEvent();
    }
}
