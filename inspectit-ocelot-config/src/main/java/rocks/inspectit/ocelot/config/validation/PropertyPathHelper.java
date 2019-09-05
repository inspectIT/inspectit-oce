package rocks.inspectit.ocelot.config.validation;

import org.springframework.beans.BeanUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.utils.CaseUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

@Component
public class PropertyPathHelper {

    /**
     * A HashSet of classes which are used as wildcards in the search for properties. If a found class matches one of these
     * classes, the end of the property path is reached. Mainly used in the search of maps
     */
    private static final HashSet<Class<?>> TERMINAL_TYPES = new HashSet(Arrays.asList(Object.class, String.class, Integer.class, Long.class,
            Float.class, Double.class, Character.class, Void.class,
            Boolean.class, Byte.class, Short.class, Duration.class, Path.class, URL.class, FileSystemResource.class));

    /**
     * Returns the type which can be found at the end of the path. Returns null if the path does not exist
     *
     * @param propertyNames The list of properties one wants to check
     * @param type          The type in which the current top-level properties should be found
     * @return The type which can be found at the end of the path. Returns null if the path does not exist
     */
    public Type getPathEndType(List<String> propertyNames, Type type) {
        if (propertyNames.isEmpty()) {
            return type;
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType genericType = (ParameterizedType) type;
            if (genericType.getRawType() == Map.class) {
                return getTypeInMap(propertyNames, genericType.getActualTypeArguments()[1]);
            } else if (genericType.getRawType() == List.class) {
                return getTypeInList(propertyNames, genericType.getActualTypeArguments()[0]);
            }
        }
        if (type instanceof Class) {
            return getTypeInBean(propertyNames, (Class<?>) type);
        } else {
            throw new IllegalArgumentException("Unexpected type: " + type);
        }
    }

    /**
     * Checks if a given type exists as value type in a map, keeps crawling through the given propertyName list
     *
     * @param propertyNames List of property names
     * @param mapValueType  The type which is given as value type of a map
     * @return True: The type exists <br> False: the type does not exists
     */
    Type getTypeInMap(List<String> propertyNames, Type mapValueType) {
        if (isTerminal(mapValueType)) {
            return mapValueType;
        } else {
            return getPathEndType(propertyNames.subList(1, propertyNames.size()), mapValueType);
        }
    }

    /**
     * Checks if a given type exists as value type in a list, keeps crawling through the given propertyName list
     *
     * @param propertyNames List of property names
     * @param listValueType The type which is given as value type of a list
     * @return True: The type exists <br> False: the type does not exists
     */
    Type getTypeInList(List<String> propertyNames, Type listValueType) {
        return getPathEndType(propertyNames.subList(1, propertyNames.size()), listValueType);
    }

    /**
     * Checks if the first entry of the propertyNames list exists as property in a given bean
     *
     * @param propertyNames List of property names
     * @param beanType      The bean through which should be searched
     * @return True: the property and all other properties exists <br> False: At least one of the properties does not exist
     */
    private Type getTypeInBean(List<String> propertyNames, Class<?> beanType) {
        String propertyName = CaseUtils.kebabCaseToCamelCase(propertyNames.get(0));
        Optional<PropertyDescriptor> foundProperty =
                Arrays.stream(BeanUtils.getPropertyDescriptors(beanType))
                        .filter(descriptor -> compareStringIgnoreCamelOrKebabCase(propertyName, descriptor.getName()))
                        .findFirst();
        if (foundProperty.isPresent()) {
            Type propertyType;
            if (foundProperty.get().getReadMethod() != null) {
                propertyType = foundProperty.get().getReadMethod().getGenericReturnType();
            } else {
                propertyType = foundProperty.get().getPropertyType();
            }
            return getPathEndType(propertyNames.subList(1, propertyNames.size()), propertyType);
        } else {
            return null;
        }
    }

    /**
     * Checks if a given type is a terminal type or an enum
     *
     * @param type
     * @return True: the given type is a terminal or an enum False: the given type is neither a terminal type nor an enum
     */
    public boolean isTerminal(Type type) {
        if (TERMINAL_TYPES.contains(type)) {
            return true;
        } else if (type instanceof Class) {
            return ((Class<?>) type).isEnum() || ((Class<?>) type).isPrimitive();
        }
        return false;
    }

    /**
     * Checks if a given type is a list of terminal types
     *
     * @param type
     * @return True: the given type is a list of a terminal type. False: either the given type is not a list or not a list of terminal types
     */
    public boolean isListOfTerminalTypes(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType genericType = (ParameterizedType) type;
            if (genericType.getRawType() == List.class) {
                return isTerminal(genericType.getActualTypeArguments()[0]);
            }
        }
        return false;
    }


    /**
     * This method takes an array of strings and returns each entry as ArrayList containing the parts of each element.
     * <p>
     * 'inspectit.hello-i-am-testing' would be returned as {'inspectit', 'helloIAmTesting'}
     *
     * @param propertyName A String containing the property path
     * @return a List containing containing the parts of the property path as String
     */
    public List<String> parse(String propertyName) {
        ArrayList<String> result = new ArrayList<>();
        String remainder = propertyName;
        while (remainder != null && !remainder.isEmpty()) {
            remainder = extractExpression(remainder, result);
        }
        return result;
    }

    /**
     * Extracts the first path expression from the given propertyName and appends it to the given result list.
     * The remainder of the property name is returned
     * <p>
     * E.g. inspectit.test.rest -> "inspectit" is added to the list, "test.rest" is returned.
     * E.g. [inspectit.literal].test.rest -> "inspectit.literal" is added to the list, "test.rest" is returned.
     * E.g. [inspectit.literal][test].rest -> "inspectit.literal" is added to the list, "[test].rest" is returned.
     *
     * @param propertyName A String with the path of a property
     * @param result       Reference to the list in which the extracted expressions should be saved in
     * @return the remaining expression
     */
    private String extractExpression(String propertyName, List<String> result) {
        if (propertyName.startsWith("[")) {
            int end = propertyName.indexOf(']');
            if (end == -1) {
                throw new IllegalArgumentException("invalid property path");
            }
            result.add(propertyName.substring(1, end));
            return removeLeadingDot(propertyName.substring(end + 1));
        } else {
            int end = findFirstIndexOf(propertyName, '.', '[');
            if (end == -1) {
                result.add(propertyName);
                return "";
            } else {
                result.add(propertyName.substring(0, end));
                return removeLeadingDot(propertyName.substring(end));
            }
        }
    }

    private int findFirstIndexOf(String propertyName, char first, char second) {
        int firstIndex = propertyName.indexOf(first);
        int secondIndex = propertyName.indexOf(second);
        if (firstIndex == -1) {
            return secondIndex;
        } else if (secondIndex == -1) {
            return firstIndex;
        } else {
            return Math.min(firstIndex, secondIndex);
        }
    }

    private String removeLeadingDot(String string) {
        if (string.startsWith(".")) {
            return string.substring(1);
        } else {
            return string;
        }
    }

    public boolean comparePaths(List<String> pathA, List<String> pathB) {
        if (pathA == null || pathB == null || pathA.size() != pathB.size()) {
            return false;
        }
        int i = 0;
        for (String pathLiteral : pathA) {
            if (!(pathLiteral.equals("*") || pathB.get(i).equals("*") || compareStringIgnoreCamelOrKebabCase(pathB.get(i), pathLiteral))) {
                return false;
            }
            i++;
        }
        return true;
    }

    /**
     * Compares two given Strings and checks if they are the same, ignores if the cases are written in different case-styles
     *
     * @param a
     * @param b
     * @return
     */
    public boolean compareStringIgnoreCamelOrKebabCase(String a, String b) {
        a = CaseUtils.kebabCaseToCamelCase(a);
        b = CaseUtils.kebabCaseToCamelCase(b);
        return a.equalsIgnoreCase(b);
    }


    /**
     * Checks if the first given path starts with the second given full path
     * <p>
     * Example: pathA = {"inspectit","instrumentation","rules"}
     * pathB = {"inspectit", "instrumentation"}
     * would return true
     *
     * @param pathA The path you want to check
     * @param pathB The path the other path should begin with
     * @return
     */
    public boolean pathAStartsLikePathB(List<String> pathA, List<String> pathB) {
        if (pathB.size() > pathA.size()) {
            return false;
        }
        int i = 0;
        for (String pathLiteral : pathA) {
            if (!(pathLiteral.equals("*") || pathB.get(i).equals("*") || compareStringIgnoreCamelOrKebabCase(pathB.get(i), pathLiteral))) {
                return false;
            }
            i++;
            if (i >= pathB.size()) {
                return true;
            }
        }
        return true;
    }

    /**
     * Returns a key from a given map, ignores if the given key and the corresponding key is written in camelCase or kebab-case
     *
     * @param map The map one wants to retrieve data from
     * @param key The key one wants to retrieve
     * @return The value found with the key
     */
    public Object getValueFromMapIgnoreCase(Map map, String key) {
        Object objectToReturn = map.get(key);
        if (objectToReturn == null) {
            return map.get(CaseUtils.kebabCaseToCamelCase(key));
        }
        return objectToReturn;
    }
}
