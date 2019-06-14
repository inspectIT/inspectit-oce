package rocks.inspectit.oce.eum.server.metrics;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CountryResponse;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.oce.eum.server.utils.IPUtils;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
/**
 * Resolves the geolocation of given IP, by using the GeoLite2 database
 * (https://dev.maxmind.com/geoip/geoip2/geolite2/)
 */
public class GeolocationResolver {
    /**
     * Location of the geoip database.
     */
    private static final String dbLocation = GeolocationResolver.class.getClassLoader().getResource("geoip-db/GeoLite2-Country.mmdb").getFile();

    /**
     * Returns country code of current requester.
     * @return country code, empty string, if country code is not resolvable
     */
    public static String getCountryCode() {
        String ip = IPUtils.getClientIpAddress();
        File database = new File(dbLocation);
        try {
            DatabaseReader dbReader = new DatabaseReader.Builder(database).build();
            InetAddress ipAddress = InetAddress.getByName(ip);
            CountryResponse response = dbReader.country(ipAddress);

            return response.getCountry().getIsoCode();

        } catch (UnknownHostException | GeoIp2Exception e) {
            log.info("The requester address {} could not be resolved", ip);
            return "";
        } catch (IOException e){
            log.warn("The geoip database could not be found!");
            return "";
        }
    }
}
