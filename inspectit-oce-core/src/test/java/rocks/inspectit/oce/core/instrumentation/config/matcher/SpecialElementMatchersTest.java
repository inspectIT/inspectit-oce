package rocks.inspectit.oce.core.instrumentation.config.matcher;

import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.StringMatcher;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.oce.core.config.model.instrumentation.scope.MethodMatcherSettings.AccessModifier;
import rocks.inspectit.oce.core.config.model.instrumentation.scope.NameMatcherSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.scope.TypeScope;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class SpecialElementMatchersTest {

    @Nested
    public class NameIs {

        @Test
        public void nullArgument() {
            ElementMatcher.Junction<NamedElement> result = SpecialElementMatchers.nameIs(null);

            assertThat(result).isNull();
        }

        @Test
        public void validSettings() {
            NameMatcherSettings settings = new NameMatcherSettings();
            settings.setName("name");
            settings.setMatcherMode(StringMatcher.Mode.MATCHES);

            ElementMatcher.Junction<NamedElement> result = SpecialElementMatchers.nameIs(settings);

            assertThat(result).isEqualTo(nameMatches("name"));
        }

        @Test
        public void emptyName() {
            NameMatcherSettings settings = new NameMatcherSettings();
            settings.setMatcherMode(StringMatcher.Mode.MATCHES);

            ElementMatcher.Junction<NamedElement> result = SpecialElementMatchers.nameIs(settings);

            assertThat(result).isNull();
        }
    }

    @Nested
    public class ArgumentsAre {

        @Test
        public void nullArgument() {
            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.argumentsAre(null);

            assertThat(result).isNull();
        }

        @Test
        public void emptyArguments() {
            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.argumentsAre(Collections.emptyList());

            assertThat(result).isEqualTo(takesArguments(0));
        }

        @Test
        public void singleArgument() {
            List<String> arguments = Arrays.asList("class0");

            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.argumentsAre(arguments);

            Object expectedResult = takesArguments(1).and(takesArgument(0, named("class0")));
            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        public void multipleArguments() {
            List<String> arguments = Arrays.asList("class0", "class1");

            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.argumentsAre(arguments);

            Object expectedResult = takesArguments(2).and(takesArgument(0, named("class0"))).and(takesArgument(1, named("class1")));
            assertThat(result).isEqualTo(expectedResult);
        }
    }

    @Nested
    public class VisibilityIs {

        @Test
        public void nullArgument() {
            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.visibilityIs(null);

            assertThat(result).isNull();
        }

        @Test
        public void emptyModifier() {
            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.visibilityIs(Collections.emptyList());

            assertThat(result).isNull();
        }

        @Test
        public void onlyPublic() {
            List<AccessModifier> modifiers = Collections.singletonList(AccessModifier.PUBLIC);

            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.visibilityIs(modifiers);

            Object expectedResult = isPublic();
            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        public void onlyPrivate() {
            List<AccessModifier> modifiers = Collections.singletonList(AccessModifier.PRIVATE);

            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.visibilityIs(modifiers);

            Object expectedResult = isPrivate();
            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        public void onlyProtected() {
            List<AccessModifier> modifiers = Collections.singletonList(AccessModifier.PROTECTED);

            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.visibilityIs(modifiers);

            Object expectedResult = isProtected();
            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        public void onlyPackage() {
            List<AccessModifier> modifiers = Collections.singletonList(AccessModifier.PACKAGE);

            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.visibilityIs(modifiers);

            Object expectedResult = not(isPublic().or(isProtected()).or(isPrivate()));
            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        public void multipleModifiers() {
            List<AccessModifier> modifiers = Arrays.asList(AccessModifier.PUBLIC, AccessModifier.PRIVATE);

            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.visibilityIs(modifiers);

            Object expectedResult = isPublic().or(isPrivate());
            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        public void eachModifier() {
            List<AccessModifier> modifiers = Arrays.asList(AccessModifier.PUBLIC, AccessModifier.PRIVATE, AccessModifier.PROTECTED, AccessModifier.PACKAGE);

            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.visibilityIs(modifiers);

            assertThat(result).isNull();
        }

        @Test
        public void duplicateModifiers() {
            List<AccessModifier> modifiers = Arrays.asList(AccessModifier.PUBLIC, AccessModifier.PUBLIC);

            Object result = SpecialElementMatchers.visibilityIs(modifiers);

            assertThat(result).isEqualTo(isPublic());
        }
    }

    @Nested
    public class OnlyOverridenMethodsOf {

        @Test
        public void nullArgument() {
            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.onlyOverridenMethodsOf(null);

            assertThat(result).isNull();
        }

        @Test
        public void nullInterfacesAndSuperclass() {
            TypeScope scope = new TypeScope();

            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.onlyOverridenMethodsOf(scope);

            assertThat(result).isNull();
        }

        @Test
        public void onlyInterface() {
            NameMatcherSettings interfaceSettings = new NameMatcherSettings();
            interfaceSettings.setName("interface1");

            TypeScope scope = new TypeScope();
            scope.setInterfaces(Collections.singletonList(interfaceSettings));

            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.onlyOverridenMethodsOf(scope);

            Object expectedResult = isOverriddenFrom(named("interface1").and(isInterface()));
            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        public void onlySuperclass() {
            NameMatcherSettings superclassSettings = new NameMatcherSettings();
            superclassSettings.setName("superclass1");

            TypeScope scope = new TypeScope();
            scope.setSuperclass(superclassSettings);

            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.onlyOverridenMethodsOf(scope);

            Object expectedResult = isOverriddenFrom(named("superclass1").and(not(isInterface())));
            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        public void fullScope() {
            NameMatcherSettings interfaceSettings = new NameMatcherSettings();
            interfaceSettings.setName("interface1");
            NameMatcherSettings superclassSettings = new NameMatcherSettings();
            superclassSettings.setName("superclass1");

            TypeScope scope = new TypeScope();
            scope.setInterfaces(Collections.singletonList(interfaceSettings));
            scope.setSuperclass(superclassSettings);

            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.onlyOverridenMethodsOf(scope);

            Object expectedResult = isOverriddenFrom(named("interface1").and(isInterface()).or(named("superclass1").and(not(isInterface()))));
            assertThat(result).isEqualTo(expectedResult);
        }

    }
}