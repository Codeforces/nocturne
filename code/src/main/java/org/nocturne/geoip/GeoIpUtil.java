package org.nocturne.geoip;

import com.maxmind.db.CHMCache;
import com.maxmind.db.Reader;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import org.nocturne.util.StringUtil;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.InetAddress;

/**
 * @author Mike Mirzayanov (mirzayanovmr@gmail.com)
 */
@SuppressWarnings("WeakerAccess")
public final class GeoIpUtil {
    private static final DatabaseReader COUNTRY_DETECTION_SERVICE;

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
            COUNTRY_DETECTION_SERVICE = new DatabaseReader.Builder(
                    GeoIpUtil.class.getResourceAsStream(countryResourcePath)
            ).withCache(new CHMCache()).fileMode(Reader.FileMode.MEMORY).build();
        } catch (IOException e) {
            throw new RuntimeException("Can't read resource '" + countryResourcePath + "'.", e);
        }
    }
}
