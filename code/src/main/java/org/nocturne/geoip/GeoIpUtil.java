package org.nocturne.geoip;

import com.maxmind.db.CHMCache;
import com.maxmind.db.Reader;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.nocturne.util.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.*;

/**
 * @author Mike Mirzayanov (mirzayanovmr@gmail.com)
 */
@SuppressWarnings("WeakerAccess")
public final class GeoIpUtil {
    private static final Logger logger = Logger.getLogger(GeoIpUtil.class);

    private static final DatabaseReader COUNTRY_DETECTION_SERVICE;
    private static final DatabaseReader CITY_DETECTION_SERVICE;

    private GeoIpUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * @param ip IPv4 or IPv6 ip-address.
     * @return The ISO two-letter country code of country or "--". Example: RU.
     */
    @Nonnull
    public static String getCountryCodeByIp(@Nonnull String ip) {
        try {
            return COUNTRY_DETECTION_SERVICE.country(InetAddress.getByName(ip)).getCountry().getIsoCode();
        } catch (IOException | GeoIp2Exception | RuntimeException ignored) {
            return "--";
        }
    }

    @Nullable
    public static String getCityByIp(@Nonnull String ip) {
        try {
            CityResponse cityResponse = CITY_DETECTION_SERVICE.city(InetAddress.getByName(ip));
            if (cityResponse.getCity().getName() == null) {
                return cityResponse.getCountry().getName();
            } else {
                return cityResponse.getCountry().getName() + ", " + cityResponse.getCity().getName();
            }
        } catch (IOException | GeoIp2Exception | RuntimeException ignored) {
            return null;
        }
    }

    @Nonnull
    public static Map<String, String> getCityByIp(@Nonnull Collection<String> ips) {
        Map<String, String> result = new HashMap<>();
        for (String ip : ips) {
            String city = getCityByIp(ip);
            if (city != null) {
                result.put(ip, city);
            }
        }
        return result;
    }

    /**
     * @param httpServletRequest Http request.
     * @return The ISO two-letter country code of country or "--". Example: RU.
     */
    @Nonnull
    public static String getCountryCode(@Nonnull HttpServletRequest httpServletRequest) {
        String ip = StringUtil.trimToNull(httpServletRequest.getHeader("X-Real-IP"));
        if (ip != null) {
            return getCountryCodeByIp(ip);
        }

        ip = StringUtil.trimToNull(httpServletRequest.getRemoteAddr());
        if (ip != null) {
            return getCountryCodeByIp(ip);
        }

        return "--";
    }

    static {
        String countryResourcePath = "/org/nocturne/geoip2/GeoLite2-Country.mmdb";

        try {
            COUNTRY_DETECTION_SERVICE = new DatabaseReader.Builder(GeoIpUtil.class.getResourceAsStream(
                    countryResourcePath)).withCache(new CHMCache()).fileMode(Reader.FileMode.MEMORY).build();
        } catch (IOException e) {
            throw new RuntimeException("Can't read resource '" + countryResourcePath + "'.", e);
        }

        DatabaseReader cityDatabaseReader = null;
        List<String> citiesPaths = Arrays.asList("/srv/app/GeoLite2-City.mmdb", "C:/Temp/GeoLite2-City.mmdb");
        for (String citiesPath : citiesPaths) {
            if (cityDatabaseReader == null) {
                try (InputStream cityInputStream = Files.newInputStream(new File(citiesPath).toPath())) {
                    cityDatabaseReader = new DatabaseReader.Builder(cityInputStream)
                            .withCache(new CHMCache()).fileMode(Reader.FileMode.MEMORY).build();
                    logger.info("GeoLite2-City loaded from '" + citiesPath + "'.");
                } catch (Exception e) {
                    logger.info("Can't find \"" + citiesPath + "\".");
                }
            }
        }

        if (cityDatabaseReader == null) {
            logger.warn("Can't find GeoLite2-City.mmdb in paths: " + StringUtils.join(citiesPaths, ", ") + ".");
        }

        CITY_DETECTION_SERVICE = cityDatabaseReader;
    }
}
