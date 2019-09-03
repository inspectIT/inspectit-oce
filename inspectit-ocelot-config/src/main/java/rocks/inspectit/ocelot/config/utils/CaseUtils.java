package rocks.inspectit.ocelot.config.utils;

import lombok.experimental.UtilityClass;


@UtilityClass
public class CaseUtils {

    /**
     * Converts the given camelCase String to kebab-case.
     * Any other separator characters are note affected.
     *
     * @param str the string in camelCase
     * @return the string in kebab-case
     */
    public String camelCaseToKebabCase(String str) {
        int position = 0;
        for (int i = 0; i < str.length() - 1; i++) {
            char first = str.charAt(i);
            char second = str.charAt(i + 1);
            if (Character.isLowerCase(first) && Character.isUpperCase(second)) {
                str = str.substring(0, i + 1) + "-" + Character.toLowerCase(second) + str.substring(i + 2);
            }
        }
        return str;
    }

    /**
     * Converts the given kebab-case String into camelCase
     *
     * @param name The string in kebab-case
     * @return the string in camel-case
     */
    public String kebabCaseToCamelCase(String name) {
        StringBuilder builder = new StringBuilder();
        String[] nameParts = name.split("-");
        boolean isFirst = true;
        for (String part : nameParts) {
            if (isFirst) {
                builder.append(part.toLowerCase());
                isFirst = false;
            } else if (!part.isEmpty()) {
                part = part.toLowerCase();
                part = part.substring(0, 1).toUpperCase() + part.substring(1);
                builder.append(part);
            }
        }
        return builder.toString();
    }

}
