package rocks.inspectit.oce.core.config.model.instrumentation.dataproviders;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;

import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Settings defining a generic data provider.
 * The name is defined by the key within the map
 * {@link InstrumentationSettings#getDataProviders()}
 */
@Data
@NoArgsConstructor
public class GenericDataProviderSettings {

    public static final String PACKAGE_REGEX = "[a-zA-Z]\\w*(\\.[a-zA-Z]\\w*)*";
    public static final String THIZ_VARIABLE = "thiz";
    public static final String ARGS_VARIABLE = "args";
    public static final String THROWN_VARIABLE = "thrown";
    public static final String RETURN_VALUE_VARIABLE = "returnValue";
    public static final String ARG_VARIABLE_PREFIX = "arg";
    public static final Pattern ARG_VARIABLE_PATTERN = Pattern.compile(ARG_VARIABLE_PREFIX + "(\\d+)");

    private static final List<Pattern> SPECIAL_VARIABLES_REGEXES = Arrays.asList(
            Pattern.compile(THIZ_VARIABLE),
            Pattern.compile(ARGS_VARIABLE),
            Pattern.compile(THROWN_VARIABLE),
            Pattern.compile(RETURN_VALUE_VARIABLE),
            ARG_VARIABLE_PATTERN);

    /**
     * Defines the input variables used by this data provider.
     * The key is the name of the variable, the value is the type of the corresponding variable.
     * The following "special" variables are available:
     * - thiz: the this-instance on which the instrumented method is executed.
     * - argN: the N-th argument with which the instrumented method was invoked.
     * - args: an array of all arguments with which the instrumented method was invoked, the type must be Object[]
     * - returnValue: the value returned by the instrumented method,
     * null if void, the method threw an exception or the provider is not executed at the method exit
     * - thrown: the {@link Throwable}-Object raised by the the executed method, the type must be java.lang.Throwable
     * null if no throwable was raised
     * <p>
     * In addition arbitrary custom input variables may be defined.
     */
    private Map<@NotBlank String, @NotBlank String> input = new HashMap<>();

    /**
     * A list of packages to import when compiling the java code and deriving the types of {@link #input}
     * If a classname is not found, the given packages will be scanned in the given order to locate the class.
     * This allows the User to use classes without the need to specify the FQN.
     */
    private List<@javax.validation.constraints.Pattern(regexp = PACKAGE_REGEX)
            String> imports = new ArrayList<>();

    /**
     * A single Java-statement (without return) defining the value of this data provider.
     * The statement must be of type Object, primitive results have to wrapped manually!
     * If this field is present, {@link #valueBody} must be null!
     */
    private String value;

    /**
     * A string defining the Java method body of the data-provider without surrounding braces {}.
     * This method body must have a return statement to return the value provided by the provider!
     * The statement must be of type Object, primitive results have to wrapped manually!
     * If this field is present, {@link #value} must be null!
     */
    private String valueBody;


    @AssertFalse(message = "Either 'value' or 'valueBody' must be present")
    private boolean isEitherValueOrValueBodyPresent() {
        return StringUtils.isEmpty(value) && StringUtils.isEmpty(valueBody);
    }

    @AssertFalse(message = "'value' and 'valueBody' cannot be both specified!")
    private boolean isNotValueAndValueBodyPresent() {
        return !StringUtils.isEmpty(value) && !StringUtils.isEmpty(valueBody);
    }

    @AssertTrue(message = "The 'args' input must have the type 'Object[]'")
    private boolean isArgsArrayTypeCorrect() {
        String argsType = input.get(ARGS_VARIABLE);
        return argsType == null || argsType.equals("Object[]") || argsType.equals("java.lang.Object[]");
    }

    @AssertTrue(message = "The 'thrown' input must have the type 'Throwable'")
    private boolean isThrownTypeCorrect() {
        String thrownType = input.get(THROWN_VARIABLE);
        return thrownType == null || thrownType.equals("java.lang.Throwable") || thrownType.equals("Throwable");
    }

    public static boolean isSpecialVariable(String varName) {
        return SPECIAL_VARIABLES_REGEXES.stream().anyMatch(p -> p.matcher(varName).matches());
    }

    public Class<?> locateTypeWithinImports(String typename, ClassLoader context) {
        return Stream.concat(Stream.concat(
                Stream.of(""),
                imports.stream().map(s -> s + ".")),
                Stream.of("java.lang."))
                .flatMap(prefix -> {
                    try {
                        return Stream.of(Class.forName(prefix + typename, false, context));
                    } catch (Exception e) {
                        return Stream.empty();
                    }
                }).findFirst().orElse(null);
    }
}
