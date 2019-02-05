package rocks.inspectit.oce.core.instrumentation.config.matcher;

import net.bytebuddy.matcher.ElementMatcher;

import static net.bytebuddy.matcher.ElementMatchers.not;

/**
 * Helper class for building and linking {@link ElementMatcher}s. All of the methods are null-safe. Adding a null matcher
 * will not have an effect on the matcher in construction.
 *
 * @param <T> The matchers generic type. Most of the time it will be TypeDescription or MethodDescription.
 */
public class MatcherChainBuilder<T> {

    private ElementMatcher.Junction<T> matcher = null;

    public void or(ElementMatcher.Junction<T> nextMatcher) {
        if (nextMatcher != null) {
            if (matcher == null) {
                matcher = nextMatcher;
            } else {
                matcher = matcher.or(nextMatcher);
            }
        }
    }

    public void and(ElementMatcher.Junction<T> nextMatcher) {
        if (nextMatcher != null) {
            if (matcher == null) {
                matcher = nextMatcher;
            } else {
                matcher = matcher.and(nextMatcher);
            }
        }
    }

    public void and(boolean condition, ElementMatcher.Junction<T> nextMatcher) {
        if (nextMatcher != null) {
            if (condition) {
                and(nextMatcher);
            } else {
                and(not(nextMatcher));
            }
        }
    }

    public ElementMatcher.Junction<T> build() {
        return matcher;
    }

    public boolean isEmpty() {
        return matcher == null;
    }
}
