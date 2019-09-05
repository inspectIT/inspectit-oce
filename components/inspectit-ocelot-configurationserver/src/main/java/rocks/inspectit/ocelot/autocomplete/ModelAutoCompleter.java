package rocks.inspectit.ocelot.autocomplete;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.utils.CaseUtils;
import rocks.inspectit.ocelot.config.validation.PropertyPathHelper;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ModelAutoCompleter implements AutoCompleter {

    @Autowired
    private PropertyPathHelper help;

    @Override
    public List<String> getSuggestions(List<String> path) {
        return collectProperties(path.subList(1, path.size()));
    }

    /**
     * Returns the names of the properties in a given path
     *
     * @param propertyPath The path to a property one wants to recieve the properties of
     * @return The names of the properties of the given path as list
     */
    private List<String> collectProperties(List<String> propertyPath) {
        Type endType = help.getPathEndType(propertyPath, InspectitConfig.class);
        if (endType == null || help.isTerminal(endType) || help.isListOfTerminalTypes(endType) || !(endType instanceof Class<?>)) {
            return new ArrayList<>();
        }
        return getProperties((Class<?>) endType);
    }

    /**
     * Return the properties of a given class
     *
     * @param beanClass the class one wants the properties of
     * @return the properties of the given class
     */
    @VisibleForTesting
    List<String> getProperties(Class<?> beanClass) {
        return Arrays.stream(BeanUtils.getPropertyDescriptors(beanClass))
                .map(PropertyDescriptor::getName)
                .map(CaseUtils::camelCaseToKebabCase)
                .collect(Collectors.toList());
    }
}
