package rocks.inspectit.ocelot.autocomplete;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.utils.CaseUtils;
import rocks.inspectit.ocelot.config.validation.PropertyPathHelper;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@Component
public class AutoCompleterModel implements AutoCompleter {

    /**
     * A HashSet of classes which are used as wildcards in the search for properties. If a found class matches one of these
     * classes, the end of the property path is reached. Mainly used in the search of maps
     */
    private static final HashSet<Class<?>> TERMINAL_TYPES = new HashSet(Arrays.asList(Object.class, String.class, Integer.class, Long.class,
            Float.class, Double.class, Character.class, Void.class,
            Boolean.class, Byte.class, Short.class, Duration.class));

    @Autowired
    private PropertyPathHelper help;

    @Override
    public List<String> getSuggestions(List<String> camelCasePath) {
        return collectProperties(camelCasePath.subList(1, camelCasePath.size()));
    }

    /**
     * Returns the names of the properties in a given path
     *
     * @param properties The path to a property one wants to recieve the properties of
     * @return The names of the properties of the given path as list
     */
    private List<String> collectProperties(List<String> properties) {
        Type endType = help.getPathEndType(properties, InspectitConfig.class);
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
        ArrayList<String> propertyList = new ArrayList<>();
        Arrays.stream(BeanUtils.getPropertyDescriptors(beanClass)).forEach(descriptor -> propertyList.add(CaseUtils.camelCaseToKebabCase(descriptor.getName())));
        return propertyList;
    }
}
