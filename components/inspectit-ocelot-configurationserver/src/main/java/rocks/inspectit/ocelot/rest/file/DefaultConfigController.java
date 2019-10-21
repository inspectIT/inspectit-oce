package rocks.inspectit.ocelot.rest.file;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import rocks.inspectit.ocelot.config.DefaultConfigProvider;
import rocks.inspectit.ocelot.rest.AbstractBaseController;

import java.util.HashMap;

@RestController
public class DefaultConfigController extends AbstractBaseController {

    @Autowired
    protected DefaultConfigProvider defaultConfigProvider;

    @GetMapping(value = "defaultconfig")
    public HashMap<String, String> getDefaultConfigContent() {
        return defaultConfigProvider.getDefaultConfigContent();
    }

}
