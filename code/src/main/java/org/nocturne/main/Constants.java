package org.nocturne.main;

import freemarker.template.Configuration;
import freemarker.template.Version;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 10.02.15
 */
@SuppressWarnings("WeakerAccess")
public class Constants {
    public static final String CONFIGURATION_FILE = "/nocturne.properties";
    public static final Version FREEMARKER_VERSION = Configuration.VERSION_2_3_21;

    private Constants() {
        throw new UnsupportedOperationException();
    }
}
