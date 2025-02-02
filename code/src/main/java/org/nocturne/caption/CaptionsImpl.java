/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.caption;

import com.google.inject.Singleton;
import org.apache.log4j.Logger;
import org.nocturne.exception.ConfigurationException;
import org.nocturne.main.ApplicationContext;

import java.io.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * Simple implementation of Captions interface. Uses properties files to store values.
 * </p>
 * <p>
 * In the development mode it try to locate them in the directory
 * ApplicationContext.getInstance().getDebugCaptionsDir() (see nocturne.debug-captions-dir).
 * It modifies all of them if finds new caption shortcut and saves values nocturne.null except
 * for default language which will have value equals to shortcut.
 * </p>
 * <p>
 * In the production mode it just read them exactly once (on startup) and doesn't save them.
 * </p>
 *
 * @author Mike Mirzayanov
 */
@Singleton
public class CaptionsImpl implements Captions {
    private static final Logger logger = Logger.getLogger(CaptionsImpl.class);

    private static final Pattern CAPTIONS_FILE_PATTERN = Pattern.compile("captions_[\\w]{2}\\.properties");

    /**
     * Stores properties per language.
     */
    private final Map<String, Properties> propertiesMap = new Hashtable<>();

    /**
     * Magic value to store empty value.
     */
    private static final String NULL = "nocturne.null";

    /**
     * Constructs new CaptionsImpl.
     */
    public CaptionsImpl() {
        // Load properties on startup.
        loadProperties();
    }

    @Override
    public String find(String shortcut, Object... args) {
        return find(ApplicationContext.getInstance().getLocale(), shortcut, args);
    }

    @Override
    public String find(Locale locale, String shortcut, Object... args) {
        // Default locale. It is possible equals with current "locale".
        Locale defaultLocale = ApplicationContext.getInstance().getDefaultLocale();

        // Current language.
        String language = locale.getLanguage();

        // If doesn't contain locale properties?
        if (!propertiesMap.containsKey(language)) {
            // Add empty properties.
            propertiesMap.put(language, new Properties());
        }

        // It exists anyway.
        Properties properties = propertiesMap.get(language);

        // It can be absent.
        String value = properties.getProperty(shortcut);

        // No such value?
        if (value == null || value.equals(NULL)) {
            // Is it NOT default locale?
            if (defaultLocale == locale) {
                // Set default.
                properties.setProperty(shortcut, shortcut);
            }
            // Use default locale to find value.
            value = find(defaultLocale, shortcut, args);
            // Save all properties.
            if (ApplicationContext.getInstance().isDebug()) {
                saveProperties();
            }
        }

        if (args.length > 0) {
            return MessageFormat.format(value, args);
        } else {
            return value;
        }
    }

    /**
     * Synchronizes all the properties and saves them.
     */
    private void saveProperties() {
        // Find all possible keys.
        Set<String> keys = new TreeSet<>();
        for (Map.Entry<String, Properties> entry : propertiesMap.entrySet()) {
            Set<Object> names = entry.getValue().keySet();
            for (Object name : names) {
                keys.add(name.toString());
            }
        }

        // Add empty value for each key if no such found.
        for (Map.Entry<String, Properties> entry : propertiesMap.entrySet()) {
            Properties properties = entry.getValue();
            for (String key : keys) {
                if (!properties.containsKey(key)) {
                    properties.setProperty(key, NULL);
                }
            }
            // And save properties.
            save(properties, entry.getKey());
        }
    }

    /**
     * @param properties Properties to be saved in the file.
     * @param language   Language.
     */
    private static void save(Properties properties, String language) {
        File file = new File(ApplicationContext.getInstance().getDebugCaptionsDir(), getCaptionsFileName(language));
        try {
            Writer writer = new OutputStreamWriter(new FileOutputStream(file), ApplicationContext.getInstance().getCaptionFilesEncoding());
            properties.store(writer, null);
            writer.close();
        } catch (IOException e) {
            logger.error("Can't write into file " + file + '.', e);
            throw new ConfigurationException("Can't write into file " + file + '.', e);
        }
    }

    /**
     * Method loadProperties ...
     */
    private void loadProperties() {
        if (ApplicationContext.getInstance().isDebug()) {
            loadPropertiesForDebug();
        } else {
            loadPropertiesForProduction();
        }
    }

    /**
     * Method loadPropertiesForProduction ...
     */
    private void loadPropertiesForProduction() {
        if (propertiesMap.isEmpty()) {
            List<String> languages = ApplicationContext.getInstance().getAllowedLanguages();
            for (String language : languages) {
                InputStream inputStream = getClass().getResourceAsStream(getCaptionsFileName(language));
                if (inputStream != null) {
                    try {
                        Reader reader = new InputStreamReader(inputStream, ApplicationContext.getInstance().getCaptionFilesEncoding());
                        Properties properties = new Properties();
                        properties.load(reader);
                        reader.close();
                        propertiesMap.put(language, properties);
                    } catch (IOException e) {
                        logger.error("Can't load caption properties for language " + language + '.', e);
                        throw new ConfigurationException("Can't load caption properties for language " + language + '.', e);
                    }
                }
            }
        }
    }

    /**
     * Method loadPropertiesForDebug ...
     */
    private void loadPropertiesForDebug() {
        File debugCaptionsDir = new File(ApplicationContext.getInstance().getDebugCaptionsDir());

        File[] captionFiles = debugCaptionsDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return CAPTIONS_FILE_PATTERN.matcher(name).matches();
            }
        });

        propertiesMap.clear();

        for (File captionFile : captionFiles) {
            if (captionFile.isFile()) {
                Pattern pattern = Pattern.compile("captions_([\\w]{2})\\.properties");
                Matcher matcher = pattern.matcher(captionFile.getName());
                if (matcher.matches()) {
                    String language = matcher.group(1);
                    try {
                        Reader reader = new InputStreamReader(new FileInputStream(captionFile),
                                ApplicationContext.getInstance().getCaptionFilesEncoding());
                        Properties properties = new Properties();
                        properties.load(reader);
                        propertiesMap.put(language, properties);
                        reader.close();
                    } catch (IOException ignored) {
                        // No operations.
                    }
                }
            }
        }
    }

    /**
     * Method getCaptionsFileName ...
     *
     * @param language of type String
     * @return String
     */
    private static String getCaptionsFileName(String language) {
        return "/captions_" + language + ".properties";
    }
}
