package org.nocturne.misc;

import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;

/** @author Mike Mirzayanov */
public class ReloadingClassLoader extends ClassLoader {
    /** Should ReloadingClassLoader skip class. Lazy initialized. */
    private Map<String, Boolean> shouldSkip = new HashMap<String, Boolean>();

    /** Class matcher to check if it is needed to skip class. */
    private ClassMatcher matcher;

    /** Application context. Used to locate directories. */
    private ApplicationContext context;

    /** Java class extension. */
    private static final String JAVA_CLASS_EXT = ".class";

    /**
     * Creates new instance of ReloadingClassLoader.
     *
     * @param context ApplicationContext instance.
     */
    public ReloadingClassLoader(ApplicationContext context) {
        super();
        this.context = context;
        this.matcher = new ClassMatcher(context.getReloadingClassLoaderPattern());
    }

    /**
     * Returns file content.
     *
     * @param file File to read.
     * @return byte[] Bytes.
     * @throws java.io.IOException when Can't read.
     */
    private byte[] getBytes(File file) throws IOException {
        long fileLength = file.length();
        byte raw[] = new byte[(int) fileLength];
        FileInputStream fileStream = new FileInputStream(file);
        int readBytes = fileStream.read(raw);
        if (readBytes != fileLength) {
            throw new IOException("Can't read bytes from the " +
                    file.getName() + ": " + readBytes + " != " + fileLength + ".");
        }
        fileStream.close();
        return raw;
    }

    /**
     * Loads class.
     *
     * @param name Class name.
     * @param resolve Resolve.
     * @return Class Loaded class.
     * @throws ClassNotFoundException when if can't load class.
     */
    public Class loadClass(String name, boolean resolve)
            throws ClassNotFoundException {

        // Use standard class loader?
        if (isForceToLoadUsingStandardClassLoader(name)) {
            return loadUsingStandardClassLoader(name, resolve);
        }

        String classFileName = name.replace('.', '/');
        File classFile = null;

        String[] classPathItems = context.getReloadingClassLoaderClassesPath().split("\\s*;\\s*");
        for (String classPathItem: classPathItems) {
            if (!classPathItem.isEmpty()) {
                File localClassFile = new File(classPathItem, classFileName + JAVA_CLASS_EXT);
                if (localClassFile.exists()) {
                    classFile = localClassFile;
                }
            }
        }

        // Load using standard class loader if can't find class file.
        if (classFile == null) {
            return loadUsingStandardClassLoader(name, resolve);
        }

        Class clazz = findLoadedClass(name);

        try {
            byte raw[] = getBytes(classFile);
            clazz = defineClass(name, raw, 0, raw.length);
            //System.err.println("Loaded " + name);
        } catch (IOException ie) {
            // No operations.
        }

        if (clazz == null) {
            clazz = getParent().loadClass(name);
        }

        if (resolve)
            resolveClass(clazz);

        return clazz;
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
}
