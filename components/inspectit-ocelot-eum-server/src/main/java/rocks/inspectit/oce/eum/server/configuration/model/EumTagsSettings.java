package rocks.inspectit.oce.eum.server.configuration.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import rocks.inspectit.oce.eum.server.utils.DefaultTags;
import rocks.inspectit.oce.eum.server.utils.IPUtils;
import rocks.inspectit.ocelot.config.model.tags.TagsSettings;

import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.AssertTrue;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Holds an additional map of tags, which will be resolved based on the EUM beacon.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class EumTagsSettings extends TagsSettings {

    /**
     * Maps tag name to beacon key.
     */
    private final Map<String, String> beacon = new HashMap<>();

    /**
     * List of tags, which are defined as global
     */
    private final Set<String> defineAsGlobal = new HashSet<>();

    /**
     * Custom IP mapping for COUNTRY_CODE
     */
    private final Map<String, List<String>> customIPMapping = new HashMap<>();

    /**
     * IPUtils
     */
    private IPUtils ipUtils = new IPUtils();

    @AssertFalse(message = "All defined global tags should exist either in extra tags or beacon tags")
    public boolean isGlobalTagMissing() {
        return defineAsGlobal.stream()
                .anyMatch(globalTag ->
                        !(getExtra().containsKey(globalTag)
                                || getBeacon().containsKey(globalTag)
                                || DefaultTags.isDefaultTag(globalTag)));
    }

    @AssertTrue(message = "Each tag should only be defined once")
    public boolean isCheckUniquenessOfTags() {
        return getExtra().keySet().stream().allMatch(extraTag -> !beacon.containsKey(extraTag));
    }

    @AssertTrue(message = "The ip addresses have to be defined in the canonical format XXX.XXX.XXX.XXX or XXX.XXX.XXX.XXX/XX")
    public boolean isCheckIpsAreFormattedCorrectly() {
        return customIPMapping.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toList()).stream().allMatch(address -> ipUtils.isIpAdresse(address));
    }

    @AssertTrue(message = "The ip definitions between the different categories must not overlap")
    public boolean isCheckIpRangesDoNotOverlap() {
      return customIPMapping.values().stream()
              .allMatch(ipList -> ipList.stream()
                      .allMatch(adresse -> customIPMapping.values().stream()
                           .allMatch(listToCompare -> listToCompare == ipList || listToCompare.stream().noneMatch(adresseToCompare -> areOverlapping(adresse, adresseToCompare)))));
    }

    /**
     * Helper method, which compares two address entries.
     * @param address1
     * @param address2
     * @return
     */
    private boolean areOverlapping(String address1, String address2){
        if(address1.contains("/") && address2.contains("/")){
            return ipUtils.overlap(address1, address2);
        } else if (address1.contains("/") && !address2.contains("/")){
            return ipUtils.containsIp(address1, address2);
        } else if (!address1.contains("/") && address2.contains("/")){
            return ipUtils.containsIp(address2, address1);
        } else {
            return address1.equals(address2);
        }
    }
}
