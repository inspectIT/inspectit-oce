package rocks.inspectit.oce.core.instrumentation.context;

import lombok.extern.slf4j.Slf4j;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Implements the logic for generating and reading the http headers related to context propagation.
 * Currently the propagation happens only via the Correlation-Context headers:
 * https://github.com/w3c/correlation-context/blob/master/correlation_context/HTTP_HEADER_FORMAT.md
 * When tracing is added, additional header formats, such as B3 will be used.
 */
@Slf4j
public class ContextPropagationUtil {

    /**
     * Maps each serializable type to its identifier.
     * If a non-string type is serialized, this d is used in the Correlation-Context Header, e.g.:
     * Correlation-Context: pi=3.14;type=d
     * (d is the identifier for "Double")
     */
    private static final Map<Class<?>, Character> TYPE_TO_ID_MAP = new HashMap<>();
    private static final Map<Character, Function<String, Object>> TYPE_ID_TO_PARSER_MAP = new HashMap<>();

    private static final String ENCODING_CHARSET = java.nio.charset.StandardCharsets.UTF_8.toString();

    public static final String CORRELATION_CONTEXT_HEADER = "Correlation-Context";

    private static final Set<String> PROPAGATION_FIELDS = new HashSet<>(Collections.singletonList(CORRELATION_CONTEXT_HEADER));

    private static final Pattern COMMA_WITH_WHITESPACES = Pattern.compile(" *, *");
    private static final Pattern SEMICOLON_WITH_WHITESPACES = Pattern.compile(" *; *");
    private static final Pattern EQUALS_WITH_WHITESPACES = Pattern.compile(" *= *");

    static {
        TYPE_TO_ID_MAP.put(Byte.class, 'a'); //use a because b is already taken for boolean
        TYPE_ID_TO_PARSER_MAP.put('a', Byte::parseByte);
        TYPE_TO_ID_MAP.put(Short.class, 's');
        TYPE_ID_TO_PARSER_MAP.put('s', Short::parseShort);
        TYPE_TO_ID_MAP.put(Integer.class, 'i');
        TYPE_ID_TO_PARSER_MAP.put('i', Integer::parseInt);
        TYPE_TO_ID_MAP.put(Long.class, 'l');
        TYPE_ID_TO_PARSER_MAP.put('l', Long::parseLong);
        TYPE_TO_ID_MAP.put(Float.class, 'f');
        TYPE_ID_TO_PARSER_MAP.put('f', Float::parseFloat);
        TYPE_TO_ID_MAP.put(Double.class, 'd');
        TYPE_ID_TO_PARSER_MAP.put('d', Double::parseDouble);
        TYPE_TO_ID_MAP.put(Character.class, 'c');
        TYPE_ID_TO_PARSER_MAP.put('c', s -> s.charAt(0));
        TYPE_TO_ID_MAP.put(Boolean.class, 'b');
        TYPE_ID_TO_PARSER_MAP.put('b', Boolean::parseBoolean);
    }

    /**
     * Takes the given key-value pairs and encodes them into the Correlation-Context header.
     *
     * @param dataToPropagate the key-value pairs to propagate.
     * @return the result propagation map
     */
    public static Map<String, String> buildPropagationHeaderMap(Stream<Map.Entry<String, Object>> dataToPropagate) {
        StringBuilder contextCorrelationData = new StringBuilder();
        dataToPropagate.forEach(e -> {
            try {
                Object value = e.getValue();
                Character typeId = TYPE_TO_ID_MAP.get(value.getClass());
                if (value instanceof String || typeId != null) {
                    String key = e.getKey();
                    String encodedValue = URLEncoder.encode(value.toString(), ENCODING_CHARSET);
                    String encodedKey = URLEncoder.encode(key, ENCODING_CHARSET);
                    if (contextCorrelationData.length() > 0) {
                        contextCorrelationData.append(',');
                    }
                    contextCorrelationData.append(encodedKey).append('=').append(encodedValue);
                    if (typeId != null) {
                        contextCorrelationData.append(";type=").append(typeId);
                    }
                }
            } catch (Throwable t) {
                log.error("Error encoding correlation context header", e);
            }
        });
        if (contextCorrelationData.length() > 0) {
            return Collections.singletonMap(CORRELATION_CONTEXT_HEADER, contextCorrelationData.toString());
        } else {
            return Collections.emptyMap();
        }
    }

    /**
     * Returns all header names which can potentially be output by {@link #buildPropagationHeaderMap(Stream)}.
     *
     * @return the set of header names
     */
    public static Set<String> getPropagationHeaderNames() {
        return PROPAGATION_FIELDS;
    }

    /**
     * Decodes te given header to value map into the given target context.
     *
     * @param propagationMap the headers to decode
     * @param target         the context in which the decoded data key-value pairs will be stored.
     */
    public static void readPropagationHeaderMap(Map<String, String> propagationMap, InspectitContext target) {
        if (propagationMap.containsKey(CORRELATION_CONTEXT_HEADER)) {
            readCorrelationContext(propagationMap.get(CORRELATION_CONTEXT_HEADER), target);
        }
    }

    /**
     * Parses the value of the Correlation-Context header, storing the propagated data values into the target context.
     *
     * @param correlationContext the value of the Correlation-Context header
     * @param target             the target context in which the data will be stored
     */
    private static void readCorrelationContext(String correlationContext, InspectitContext target) {
        correlationContext = correlationContext.trim();
        for (String keyValuePair : COMMA_WITH_WHITESPACES.split(correlationContext)) {
            try {
                //split into assignment and attributes
                String[] pairAndProperties = SEMICOLON_WITH_WHITESPACES.split(keyValuePair);
                String[] keyAndValue = EQUALS_WITH_WHITESPACES.split(pairAndProperties[0]);
                if (keyAndValue.length != 2) {
                    continue;
                }
                String key = URLDecoder.decode(keyAndValue[0], ENCODING_CHARSET);
                String stringValue = URLDecoder.decode(keyAndValue[1], ENCODING_CHARSET);
                List<String> properties = Arrays.asList(pairAndProperties).subList(1, pairAndProperties.length);
                Object resultValue = parseTyped(stringValue, properties);
                target.setData(key, resultValue);
            } catch (Throwable t) {
                log.error("Error decoding Correlation-Context header", t);
            }
        }
    }

    /**
     * Scans the given properties for a type=... definition.
     * If a correct definition is found, the given string value is parsed ot the given type and returned.
     * Otherwise the string value is returned unchanged.
     *
     * @param stringValue the value to parse
     * @param properties  the collection of proeprty definition in the format "propertyname=value"
     * @return the parsed value
     */
    private static Object parseTyped(String stringValue, Collection<String> properties) {
        for (String property : properties) {
            String[] propertyNameAndValue = EQUALS_WITH_WHITESPACES.split(property);
            if (propertyNameAndValue.length == 2) {
                String propertyName = propertyNameAndValue[0];
                String propertyValue = propertyNameAndValue[1];
                if (propertyName.equals("type") && propertyValue.length() == 1) {
                    Function<String, Object> parser = TYPE_ID_TO_PARSER_MAP.get(propertyValue.charAt(0));
                    if (parser != null) {
                        return parser.apply(stringValue);
                    }
                }
            }
        }
        return stringValue;
    }


}
