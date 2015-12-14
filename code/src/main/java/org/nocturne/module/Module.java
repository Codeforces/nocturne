/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.module;

import freemarker.cache.TemplateLoader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.nocturne.exception.ModuleInitializationException;
import org.nocturne.main.ApplicationContext;
import org.nocturne.util.FileUtil;
import org.nocturne.util.StreamUtil;

import javax.servlet.ServletContext;
import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

/**
 * Stores information about the module.
 *
 * @author Mike Mirzayanov.
 */
public class Module {
    private static final Pattern MODULE_URL_MATCH_PATTERN = Pattern.compile("module-.*\\.jar");

    /**
     * Module name.
     */
    private String name;

    /**
     * Module jar file in WEB-INF/lib (or in other directory of classpath)
     */
    private final JarFile file;

    /**
     * Directories for debug.
     */
    private final DebugContext debugContext = new DebugContext();

    /**
     * Module priority. Modules with highter priority will be loaded later and can override previously loaded modules setup.
     */
    private int priority;

    /**
     * Module freemarker template loader.
     */
    private TemplateLoader templateLoader;

    /**
     * Module resource loader.
     */
    private ResourceLoader resourceLoader;

    /**
     * Specific module configuration. Each module contains its own implementation of this interface.
     */
    private Configuration configuration;

    /**
     * Each module can specify implementation of Runnable to be loaded on module startup (once).
     */
    private String startupClassName;

    private static ApplicationContext getApplicationContext() {
        return ApplicationContext.getInstance();
    }

    /**
     * @param url URL containing path to module JAR-file.
     */
    public Module(URL url) {
        try {
            file = new JarFile(FileUtils.toFile(url));
        } catch (IOException e) {
            throw new ModuleInitializationException("Can't create JarFile instance from " + url + '.', e);
        }
    }

    /**
     * @return Each module can specify implementation of Runnable to be loaded on
     */
    public String getStartupClassName() {
        return startupClassName;
    }

    /**
     * @return Specific module configuration. Each module contains its own implementation of this interface.
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * @return Module name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return Module freemarker template loader.
     */
    public TemplateLoader getTemplateLoader() {
        return templateLoader;
    }

    /**
     * @return Module resource loader.
     */
    public ResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    /**
     * @return Module priority. Modules with higher priority will be loaded later and can override previously loaded modules setup.
     */
    public int getPriority() {
        return priority;
    }

    /**
     * @return Module jar file in WEB-INF/lib (or in other directory of classpath).
     */
    public JarFile getFile() {
        return file;
    }

    /**
     * @return Directories for debug.
     */
    public DebugContext getDebugContext() {
        return debugContext;
    }

    /**
     * Internal nocturne method to read module properties and construct Module instance completely.
     */
    public void init() {
        JarEntry webappEntry = file.getJarEntry("module.xml");

        if (webappEntry.isDirectory()) {
            throw new ModuleInitializationException("Entry module.xml should be file in module " + file.getName() + '.');
        }

        InputStream inputStream = null;
        try {
            inputStream = file.getInputStream(webappEntry);

            byte[] moduleXmlBytes = StreamUtil.getAsByteArray(inputStream);

            if (getApplicationContext().isDebug()) {
                initializeForDebug(moduleXmlBytes);

                setupTemplateLoader();
                setupResourceLoader();

                getApplicationContext().addReloadingClassPath(new File(debugContext.getClassesDir()));
            } else {
                initializeForProduction(moduleXmlBytes);
            }

            setupPriority(moduleXmlBytes);
            setupConfigurationClassName(moduleXmlBytes);
            setupStartupClassName(moduleXmlBytes);
            setupName(moduleXmlBytes);
        } catch (IOException e) {
            throw new ModuleInitializationException("Can't perform IO operation [module=" + file.getName() + "].", e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private void setupStartupClassName(byte[] moduleXmlBytes) {
        try {
            startupClassName = FileUtil.extractFromXml(
                    new ByteArrayInputStream(moduleXmlBytes),
                    "/module/properties/startup-class",
                    String.class
            );
        } catch (Exception ignored) {
            // Optional parameter.
        }
    }

    private void setupConfigurationClassName(byte[] moduleXmlBytes) {
        try {
            String configurationClassName = FileUtil.extractFromXml(
                    new ByteArrayInputStream(moduleXmlBytes),
                    "/module/properties/configuration-class",
                    String.class
            );

            configuration = (Configuration) getClass().getClassLoader().loadClass(configurationClassName).getConstructor().newInstance();
        } catch (Exception e) {
            throw new ModuleInitializationException("Can't find element /module/properties/configuration-class " +
                    "or it contains illegal value.", e);
        }
    }

    private void setupName(byte[] moduleXmlBytes) {
        name = FileUtil.extractFromXml(new ByteArrayInputStream(moduleXmlBytes), "/module/name", String.class);
    }

    private void setupResourceLoader() {
        resourceLoader = new FileResourceLoader(new File(debugContext.getWebappDir()));
    }

    private void setupTemplateLoader() {
        try {
            templateLoader = new PreprocessFreemarkerFileTemplateLoader(
                    new File(debugContext.getTemplateDir())
            );
        } catch (IOException e) {
            throw new ModuleInitializationException("Can't create module template loader.", e);
        }
    }

    private void setupPriority(byte[] moduleXmlBytes) {
        try {
            String priority = FileUtil.extractFromXml(
                    new ByteArrayInputStream(moduleXmlBytes),
                    "/module/properties/priority",
                    String.class
            ).trim();

            if (priority.isEmpty()) {
                this.priority = 1;
            } else {
                this.priority = Integer.valueOf(priority);
            }
        } catch (Exception e) {
            throw new ModuleInitializationException("Can't find or parse /module/properties/priority. " +
                    "It expected to be an integer.", e);
        }
    }

    @SuppressWarnings({"unchecked", "AccessOfSystemProperties", "OverlyStrongTypeCast", "UseOfPropertiesAsHashtable"})
    private void initializeForDebug(byte[] moduleXmlBytes) {
        String webappDir = FileUtil.extractFromXml(
                new ByteArrayInputStream(moduleXmlBytes),
                "/module/debug/directories/webapp",
                String.class
        );

        String templatesDir = FileUtil.extractFromXml(
                new ByteArrayInputStream(moduleXmlBytes),
                "/module/debug/directories/templates",
                String.class
        );

        String classesDir = FileUtil.extractFromXml(
                new ByteArrayInputStream(moduleXmlBytes),
                "/module/debug/directories/classes",
                String.class
        );

        if ("true".equalsIgnoreCase(System.getProperty("dreamcatcher.loaded"))) {
            ((Set<String>) System.getProperties().get("dreamcatcher.listen-directories")).add(classesDir);
        }

        debugContext.setWebappDir(webappDir);
        debugContext.setTemplateDir(templatesDir);
        debugContext.setClassesDir(classesDir);
    }

    private void initializeForProduction(byte[] moduleXmlBytes) throws IOException {
        String webappDir = FileUtil.extractFromXml(
                new ByteArrayInputStream(moduleXmlBytes), "/module/directories/webapp", String.class
        );

        String templatesDir = FileUtil.extractFromXml(
                new ByteArrayInputStream(moduleXmlBytes), "/module/directories/templates", String.class
        );

        String webInfDir = FileUtil.extractFromXml(
                new ByteArrayInputStream(moduleXmlBytes), "/module/directories/WEB-INF", String.class
        );

        ServletContext servletContext = getApplicationContext().getServletContext();
        String[] templatePaths = getApplicationContext().getTemplatePaths();

        copyFiles(servletContext, new File(webappDir), new File("."));
        copyFiles(servletContext, new File(templatesDir), new File(templatePaths[templatePaths.length - 1]));
        copyFiles(servletContext, new File(webInfDir), new File("WEB-INF"));
    }

    private void copyFiles(ServletContext servletContext, File sourceDir,
                           File targetDir) throws IOException {
        Enumeration<JarEntry> entries = file.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();

            File entryPathFile = new File(entry.getName());
            boolean matched = false;
            while (entryPathFile != null) {
                if (sourceDir.equals(entryPathFile)) {
                    matched = true;
                    break;
                }
                entryPathFile = entryPathFile.getParentFile();
            }

            if (!matched) {
                continue;
            }

            if (entry.isDirectory()) {
                File relativeFile = new File(targetDir, cutDir(sourceDir, new File(entry.getName())).getPath());

                String realPath = FileUtil.getRealPath(servletContext, relativeFile.getPath());
                if (realPath == null) {
                    throw new ModuleInitializationException("Path '" + relativeFile.getPath()
                            + "' expected to be a directory in servletContext.");
                }

                File entryDir = new File(realPath);

                if (!entryDir.isDirectory()) {
                    if (entryDir.isFile()) {
                        throw new ModuleInitializationException("Path " + entryDir + " expected " +
                                "to be a directory by " + file.getName() + '.');
                    } else {
                        if (!entryDir.mkdirs()) {
                            throw new ModuleInitializationException("Can't create " + entryDir + " for " +
                                    "module " + file.getName() + '.');
                        }
                    }
                }
            } else {
                File relativeFile = new File(targetDir, cutDir(sourceDir, new File(entry.getName())).getPath());

                String realPath = FileUtil.getRealPath(servletContext, relativeFile.getPath());
                if (realPath == null) {
                    throw new ModuleInitializationException("Path '" + relativeFile.getPath() + "' expected to be a found in servletContext.");
                }

                File entryFile = new File(realPath);

                //noinspection ResultOfMethodCallIgnored
                entryFile.getParentFile().mkdirs();
                StreamUtil.copyInputStream(
                        file.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(entryFile))
                );
            }
        }
    }

    private static File cutDir(File dir, File file) {
        return new File(file.getPath().substring(dir.getPath().length()));
    }

    /**
     * Checks if given URL contains JAR-file with nocturne module.
     *
     * @param url Url to check.
     * @return {@code true} iff given URL contains JAR-file with nocturne module.
     */
    public static boolean isModuleUrl(URL url) {
        File file = new File(url.getFile());
        return MODULE_URL_MATCH_PATTERN.matcher(file.getName()).matches();
    }

    /**
     * Directories where to find files of the modules which can be reloaded in the development.
     */
    static class DebugContext {
        private String webappDir;
        private String templateDir;
        private String classesDir;

        private void setWebappDir(String webappDir) {
            this.webappDir = webappDir;
        }

        private void setTemplateDir(String templateDir) {
            this.templateDir = templateDir;
        }

        public String getWebappDir() {
            return webappDir;
        }

        public String getTemplateDir() {
            return templateDir;
        }

        public String getClassesDir() {
            return classesDir;
        }

        public void setClassesDir(String classesDir) {
            this.classesDir = classesDir;
        }
    }
}
