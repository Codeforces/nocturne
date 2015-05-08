package org.nocturne.geoip;

import com.maxmind.geoip.LookupService;
import org.apache.commons.io.IOUtils;
import org.nocturne.util.StringUtil;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author Mike Mirzayanov (mirzayanovmr@gmail.com)
 */
public class GeoipUtil {
    private static final LookupService LOOKUP_COUNTRY_SERVICE;

    private GeoipUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * @param ip IPv4 or IPv6 ip-address.
     * @return The ISO two-letter country code of country or "--". Example: RU.
     */
    @Nonnull
    public static String getCountryCodeByIp(@Nonnull String ip) {
        if (ip.contains(":")) {
            return LOOKUP_COUNTRY_SERVICE.getCountryV6(ip).getCode();
        } else {
            return LOOKUP_COUNTRY_SERVICE.getCountry(ip).getCode();
        }
    }

    /**
     * @param httpServletRequest Http request.
     * @return The ISO two-letter country code of country or "--". Example: RU.
     */
    @Nonnull
    public static String getCountryCode(@Nonnull HttpServletRequest httpServletRequest) {
        String ip = httpServletRequest.getHeader("X-Real-IP");
        if (!StringUtil.isEmpty(ip)) {
            return getCountryCodeByIp(ip);
        }

        ip = httpServletRequest.getRemoteAddr();
        if (!StringUtil.isEmpty(ip)) {
            return getCountryCodeByIp(ip);
        }

        return "--";
    }

    static {
        String countryResourcePath = "/org/nocturne/geoip/GeoIP.dat";

        File geoIpFile = null;
        try {
            geoIpFile = File.createTempFile("org.nocturne.geoip.GeoIP.dat", "");
            try (FileOutputStream geoIpFileOutputStream = new FileOutputStream(geoIpFile)) {
                IOUtils.copy(GeoipUtil.class.getResourceAsStream(countryResourcePath), geoIpFileOutputStream);
            }
            LOOKUP_COUNTRY_SERVICE = new LookupService(
                    geoIpFile,
                    LookupService.GEOIP_MEMORY_CACHE
            );
        } catch (IOException e) {
            throw new RuntimeException("Can't read GeoIP.dat from resource '" + countryResourcePath + "'.", e);
        } finally {
            if (geoIpFile != null) {
                //noinspection ResultOfMethodCallIgnored
                geoIpFile.delete();
            }
        }
    }
}
