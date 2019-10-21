package rocks.inspectit.ocelot.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.loaders.DefaultConfigFileLoader;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;

/**
 * This class provides a map containing all default config files.
 */
@Data
@Component
public class DefaultConfigProvider {
    @Autowired
    private DefaultConfigFileLoader defaultConfigFileLoader;

    /**
     * This Map is used to store the default config files in. The keys resemble the file paths, the values
     * resemble the file's content.
     */
    private HashMap<String, String> defaultConfigContent;

    /**
     * Loads the default config files and stores them in the defaultConfigContent variable.
     */
    @PostConstruct
    public void loadConfig() throws IOException {
        defaultConfigContent = defaultConfigFileLoader.loadDefaultConfig();
    }
}

