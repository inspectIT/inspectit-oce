package rocks.inspectit.oce.eum.server.utils;

import io.opencensus.internal.StringUtils;
import io.opencensus.tags.TagValue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TagUtils {

    private static boolean isWarningPrinted = false;

    private TagUtils() {
        // empty private default constructor for util class
    }

    /**
     * Constructs a {@code io.opencensus.tags.TagValue} from the given string.
     * If String is not valid an <code>&lt;invalid&gt;</code> TagName is created.
     *
     * @param v the tag value
     * @return the created TagValue with 'v' or '&lt;invalid&gt;'
     */
    public static TagValue createTagValue(String v) {
        if (isTagValueValid(v)) {
            return TagValue.create(v);
        }
        printWarningOnce(v);
        return TagValue.create("<invalid>");
    }

    private static boolean isTagValueValid(String value) {
        return value.length() <= TagValue.MAX_LENGTH && StringUtils.isPrintableString(value);
    }

    private static void printWarningOnce(String v) {
        if (!isWarningPrinted) {
            log.warn("illegal tag value: <" + v + "> converted to <invalid>");
            isWarningPrinted = true;
        }
    }

}
