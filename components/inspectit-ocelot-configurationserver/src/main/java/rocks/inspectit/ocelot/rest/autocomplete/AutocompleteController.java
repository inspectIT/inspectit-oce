package rocks.inspectit.ocelot.rest.autocomplete;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import rocks.inspectit.ocelot.autocomplete.AutoCompleteRequest;
import rocks.inspectit.ocelot.autocomplete.AutoCompleter;
import rocks.inspectit.ocelot.config.validation.PropertyPathHelper;
import rocks.inspectit.ocelot.rest.AbstractBaseController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The rest controller providing the interface used by the frontend server for autocomplete function.
 */
@RestController
public class AutocompleteController extends AbstractBaseController {

    @Autowired
    List<AutoCompleter> completers;

    @Autowired
    PropertyPathHelper help;

    @ApiOperation(value = "String which should be autocompleted")
    @ApiResponse(code = 200, message = "The options which you can enter into the string", examples =
    @Example(value = @ExampleProperty(value = "[\"interfaces\",\n" +
            "    \"superclass\",\n" +
            "    \"type\",\n" +
            "    \"methods\",\n" +
            "    \"advanced\"]", mediaType = "text/plain")))
    @PostMapping("/autocomplete")
    public List<String> getPossibleProperties(@RequestBody AutoCompleteRequest request) {
        ArrayList<String> ret = new ArrayList<>();
        for (AutoCompleter completer : completers) {
            completer.getSuggestions(help.parse(request.getPath())).stream().filter(ps -> !ret.contains(ps)).collect(Collectors.toList()).forEach(ps -> ret.add(ps));
        }
        return ret;
    }
}


