package rocks.inspectit.oce.eum.server.beacon.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.eum.server.beacon.Beacon;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link BeaconProcessor} to remove query string parameter from Boomerangs <b>u</b> and <b>pgu</b> Fields.
 * New beacon properties <b>u_no_query</b> and <b>pgu_no_query</b> are added.
 */
@Slf4j
@Component
public class URLNormalizeBeaconProcessor implements BeaconProcessor {

    public static String TAG_U_NO_QUERY = "U_NO_QUERY";
    public static String TAG_PGU_NO_QUERY = "PGU_NO_QUERY";


    @Override
    public Beacon process(Beacon beacon) {
        Map<String, String> uris = new HashMap<>();
        uris.put(TAG_U_NO_QUERY, resolveUrlWithoutParameter("u", beacon));
        uris.put(TAG_PGU_NO_QUERY, resolveUrlWithoutParameter("pgu", beacon));
        return beacon.merge(uris);
    }


    private String resolveUrlWithoutParameter(String sourceProperty, Beacon beacon) {
        if (beacon.contains(sourceProperty)) {
            String url = beacon.get(sourceProperty);
            try {
                URI uri = new URI(url);
                return new URI(uri.getScheme(),
                        uri.getAuthority(),
                        uri.getPath(),
                        null,
                        null)
                        .toString();
            } catch (URISyntaxException e) {
                log.error("Failed to convert url: <{}> to URI", url, e);
            }
        }
        return "";
    }
}
