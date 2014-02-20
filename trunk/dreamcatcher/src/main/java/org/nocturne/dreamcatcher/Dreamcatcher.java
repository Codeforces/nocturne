package org.nocturne.dreamcatcher;

import java.io.*;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Mike Mirzayanov (mirzayanovmr@gmail.com)
 */
public class Dreamcatcher implements DirectoryListener.Handler {
    private static final List<Dreamcatcher> dreamcatchers = Collections.synchronizedList(new ArrayList<Dreamcatcher>());
    private static final String NOCTURNE_UNUSED_RELOADING_CLASS_LOADERS = "nocturne.unused-reloading-class-loaders";
    private static final String DREAMCATCHER_LISTEN_DIRECTORIES = "dreamcatcher.listen-directories";
    private static final String CLASS_EXT = ".class";
    private static final String RELOADING_CLASS_LOADER_NAME_PREFIX = "org.nocturne.main.ReloadingClassLoader.DelegationClassLoader";

    private final Set<String> listenDirectories = new HashSet<>();
    private final Set<File> listenDirectoryFiles = new HashSet<>();
    private final Lock listenDirectoriesLock = new ReentrantLock();
    private final Instrumentation inst;
    private final DirectoryListener directoryListener = new DirectoryListener();

    private volatile int lastDreamcatcherListenDirectoriesSize = 0;

    public Dreamcatcher(Instrumentation inst, String args) {
        this.inst = inst;

        directoryListener.addHandler(this);
        directoryListener.start();

        startDirectoryListenerByArgs(args);

        Thread rescanForNewListenDirectoriesThread = new Thread(null, new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        listenMoreDirectoriesIfNeeded();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }, "Dreamcatcher.rescanForNewListenDirectoriesThread");
        rescanForNewListenDirectoriesThread.setDaemon(true);
        rescanForNewListenDirectoriesThread.start();
    }

    private static Set<String> getDreamcatcherListenDirectories() {
        //noinspection unchecked
        Set<String> result = (Set<String>) System.getProperties().get(DREAMCATCHER_LISTEN_DIRECTORIES);

        if (result == null) {
            System.getProperties().put(DREAMCATCHER_LISTEN_DIRECTORIES, new ConcurrentSkipListSet<String>());
            //noinspection unchecked
            result = (Set<String>) System.getProperties().get(DREAMCATCHER_LISTEN_DIRECTORIES);
        }

        return result;
    }

    private void listenDirectory(String directory) {
        listenDirectoriesLock.lock();

        try {
            if (!listenDirectories.contains(directory)) {
                File dir = new File(directory);
                if (!dir.exists() || !dir.isDirectory()) {
                    throw new RuntimeException("Argument '"
                            + directory
                            + "' expected to be directory to listen, but it is not a directory.");
                }
                directoryListener.addRootDir(dir);
                listenDirectoryFiles.add(dir);
                listenDirectories.add(directory);
                getDreamcatcherListenDirectories().add(directory);
                System.out.println("Dreamcatcher listen directory '" + directory + "'.");
            }
        } finally {
            listenDirectoriesLock.unlock();
        }
    }

    private void startDirectoryListenerByArgs(String args) {
        if (args != null && args.length() > 0) {
            for (String token : args.split(";")) {
                String directory = token.trim();
                if (!directory.isEmpty()) {
                    listenDirectory(directory);
                }
            }
        }
    }

    /**
     * Called when the agent is initialized via command line
     */
    public static void premain(String args, Instrumentation inst) {
        System.setProperty("dreamcatcher.loaded", "true");
        System.out.println("Dreamcatcher premain (" + args + ").");
        initialize(args, inst);
    }

    /**
     * Called when the agent is initialized after the jvm startup
     */
    @SuppressWarnings("UnusedDeclaration")
    public static void agentmain(String args, Instrumentation inst) {
        System.setProperty("dreamcatcher.loaded", "true");
        System.out.println("Dreamcatcher agentmain (" + args + ").");
        initialize(args, inst);
    }

    private static void initialize(String args, Instrumentation inst) {
        if (!getDreamcatcherListenDirectories().isEmpty()) {
            throw new RuntimeException("Expected empty dreamcatcher listen directories set.");
        }

        Dreamcatcher dreamcatcher = new Dreamcatcher(inst, args);
        dreamcatchers.add(dreamcatcher);
    }

    @Override
    public void onChange(File file) {
        if (file.getName().endsWith(CLASS_EXT)) {
            redefineClass(file);
        }
    }

    private static boolean equalFiles(File fileA, File fileB) {
        try {
            return fileA.getCanonicalPath().equals(fileB.getCanonicalPath())
                    && fileA.getAbsolutePath().equals(fileB.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IOException in getCanonicalPath.", e);
        }
    }

    protected void redefineClass(File classFile) {
        listenMoreDirectoriesIfNeeded();

        if (!System.getProperties().containsKey(NOCTURNE_UNUSED_RELOADING_CLASS_LOADERS)) {
            System.getProperties().put(NOCTURNE_UNUSED_RELOADING_CLASS_LOADERS, new HashSet<ClassLoader>());
        }

        //noinspection unchecked
        Set<ClassLoader> unusedReloadingClassLoaders
                = (Set<ClassLoader>) System.getProperties().get(NOCTURNE_UNUSED_RELOADING_CLASS_LOADERS);

        List<String> pathElements = new ArrayList<>();
        boolean top = false;
        File clazz = classFile;

        while (!top && clazz != null) {
            pathElements.add(clazz.getName());
            clazz = clazz.getParentFile();

            for (File listenDirectory : listenDirectoryFiles) {
                if (equalFiles(listenDirectory, clazz)) {
                    top = true;
                    break;
                }
            }
        }

        if (!top) {
            return;
        }

        Collections.reverse(pathElements);

        StringBuilder nameStringBuilder = new StringBuilder();
        for (String pathElement : pathElements) {
            if (nameStringBuilder.length() > 0) {
                nameStringBuilder.append('.');
            }
            nameStringBuilder.append(pathElement);
        }

        String name = nameStringBuilder.toString();
        if (name.endsWith(CLASS_EXT)) {
            name = name.substring(0, name.length() - CLASS_EXT.length());
        } else {
            return;
        }

        System.out.println("name=" + name);

        Class[] loadedClasses = inst.getAllLoadedClasses();
        for (Class<?> loadedClass : loadedClasses) {
            if (loadedClass.getClassLoader() == null) {
                continue;
            }

            if (unusedReloadingClassLoaders.contains(loadedClass.getClassLoader())) {
                continue;
            }

            //System.out.println(loadedClass.getClassLoader().getClass().toString() + " " + loadedClass.getClassLoader().getClass().getName() + " " + loadedClass.getClassLoader().getClass().getSimpleName() + " " + loadedClass.getClassLoader().getClass().getCanonicalName());

            if (!loadedClass.getClassLoader().getClass().getCanonicalName().startsWith(RELOADING_CLASS_LOADER_NAME_PREFIX)) {
                continue;
            }

            if (loadedClass.getName().equals(name)) {
                try {
                    System.out.println("= Ready to redefine  " + loadedClass.getName() + "@" + loadedClass.getClassLoader() + " with " + classFile);
                    ClassDefinition definition = new ClassDefinition(loadedClass, toByteArray(new FileInputStream(classFile)));
                    //noinspection RedundantArrayCreation
                    inst.redefineClasses(new ClassDefinition[]{definition});
                    System.out.println("Redefined " + loadedClass.getName() + " with " + classFile);
                } catch (IOException e) {
                    System.out.println(e.getClass() + " " + e.getMessage() + " " + name + " " + loadedClass);
                    // No operations.
                } catch (Throwable e) {
                    System.out.println("Can't redefine " + loadedClass);
                    e.printStackTrace();
                    System.setProperty("dreamcatcher.can-not-redefine-class", "true");
                }
            }
        }
    }

    private void listenMoreDirectoriesIfNeeded() {
        if (lastDreamcatcherListenDirectoriesSize < getDreamcatcherListenDirectories().size()) {
            listenDirectoriesLock.lock();

            try {
                List<String> directories = new ArrayList<>(getDreamcatcherListenDirectories());
                for (String directory : directories) {
                    listenDirectory(directory);
                }
                lastDreamcatcherListenDirectoriesSize = Math.max(lastDreamcatcherListenDirectoriesSize, directories.size());
            } finally {
                listenDirectoriesLock.unlock();
            }
        }
    }

    private static byte[] toByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        byte[] buffer = new byte[65536];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            if (bytesRead > 0) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
        }

        inputStream.close();
        return byteArrayOutputStream.toByteArray();
    }
}
