package org.nocturne.misc;

import java.util.*;
import java.io.File;
import java.net.URLClassLoader;
import java.net.URL;
import java.net.MalformedURLException;

/** @author Mike Mirzayanov */
public class ReloadingClassLoader extends ClassLoader {
    /** Should ReloadingClassLoader skip class. Lazy initialized. */
    private Map<String, Boolean> shouldSkip = new HashMap<String, Boolean>();

    /** Class matcher to check if it is needed to skip class. */
    private ClassMatcher matcher;

    /** Standard class loader class path. */
    private static final URL[] classPathUrls = ((URLClassLoader) ReloadingClassLoader.class.getClassLoader()).getURLs();

    /** Class loader for delegation. */
    private DelegationClassLoader delegationClassLoader;

    /**
     * Creates new instance of ReloadingClassLoader.
     *
     * @param context ApplicationContext instance.
     */
    public ReloadingClassLoader(ApplicationContext context) {
        this.matcher = new ClassMatcher(context.getReloadingClassLoaderPattern());

        List<URL> delegationClassLoaderClassPath = new ArrayList<URL>();
        String[] classPathItems = context.getReloadingClassLoaderClassesPath().split("\\s*;\\s*");

        for (String classPathItem : classPathItems) {
            File classPathDir = new File(classPathItem);

            if (classPathDir.isDirectory()) {
                try {
                    delegationClassLoaderClassPath.add(classPathDir.toURI().toURL());
                } catch (MalformedURLException e) {
                    throw new IllegalStateException("The path " + classPathDir.getAbsolutePath() + " is not valid URL.", e);
                }
            } else {
                throw new IllegalArgumentException("Expected to find directory for the path " + classPathItem + ".");
            }
        }

        delegationClassLoaderClassPath.addAll(Arrays.asList(classPathUrls));

        delegationClassLoader = new DelegationClassLoader(delegationClassLoaderClassPath.toArray(new URL[]{}));
    }

    /**
     * Loads class.
     *
     * @param name Class name.
     * @param resolve Resolve.
     * @return Class Loaded class.
     */
    public Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return delegationClassLoader.loadClass(name, resolve);
    }

    /**
     * Load class using standard loader.
     *
     * @param name Class name.
     * @param resolve Resolved.
     * @return Class Loaded class.
     * @throws ClassNotFoundException when Can't load class.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    private Class loadUsingStandardClassLoader(String name, boolean resolve) throws ClassNotFoundException {
        return ReloadingClassLoader.class.getClassLoader().loadClass(name);
    }

    /**
     * @param name Class name.
     * @return boolean {@code true} iff this class should be loaded by standard class.
     */
    private boolean isForceToLoadUsingStandardClassLoader(String name) {
        if (!shouldSkip.containsKey(name)) {
            shouldSkip.put(name, !matcher.match(name));
        }
        return shouldSkip.get(name);
    }

    class DelegationClassLoader extends URLClassLoader {
        public DelegationClassLoader(URL[] urls) {
            super(urls);
        }

        protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            // Use standard class loader?
            if (isForceToLoadUsingStandardClassLoader(name)) {
                return loadUsingStandardClassLoader(name, resolve);
            }

            return super.loadClass(name, resolve);
        }
    }
}
