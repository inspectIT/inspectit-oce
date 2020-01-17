package rocks.inspectit.ocelot.autocomplete.autocompleterimpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.autocomplete.AutoCompleter;
import rocks.inspectit.ocelot.autocomplete.util.YamlFileHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This AutoCompleter retrieves all rules which can be found in the present yaml-files. It is triggered by
 * the path "inspectit.instrumentation.rules"
 */
@Component
public class RuleAutoCompleter implements AutoCompleter {

    @Autowired
    private YamlFileHelper yamlFileHelper;

    private final static List<String> SCOPE_PATH = Arrays.asList("inspectit", "instrumentation", "rules");

    @Override
    public List<String> getSuggestions(List<String> path) {
        if (path.equals(SCOPE_PATH)) {
            return yamlFileHelper.extractKeysFromYamlFiles(SCOPE_PATH);
        }
        return new ArrayList<>();
    }
}
