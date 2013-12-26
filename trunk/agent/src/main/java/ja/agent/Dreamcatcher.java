package ja.agent;

import java.io.*;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.util.*;

/**
 * @author Mike Mirzayanov (mirzayanovmr@gmail.com)
 */
public class Dreamcatcher implements DirectoryListener.Handler {
    private static final List<Dreamcatcher> dreamcatchers = Collections.synchronizedList(new ArrayList<Dreamcatcher>());
    private static final String NOCTURNE_UNUSED_RELOADING_CLASS_LOADERS = "nocturne.unused-reloading-class-loaders";

    private final Instrumentation inst;

    public Dreamcatcher(Instrumentation inst, String args) {
        this.inst = inst;
        startDirectoryListener(args);
    }

    private void startDirectoryListener(String args) {
        DirectoryListener directoryListener = new DirectoryListener();
        for (String token : args.split(";")) {
            File directoryToListen = new File(token);
            if (!directoryToListen.isDirectory()) {
                throw new RuntimeException("Argument '"
                        + directoryToListen + "' expected to be directory to listen, but it is not a directory.");
            }
            directoryListener.addRootDir(directoryToListen);
        }

        directoryListener.addHandler(this);
        directoryListener.start();
    }

    /**
     * Called when the agent is initialized via command line
     */
    public static void premain(String args, Instrumentation inst) {
        System.out.println("Dreamcatcher premain (" + args + ").");
        initialize(args, inst);
    }

    /**
     * Called when the agent is initialized after the jvm startup
     */
    public static void agentmain(String args, Instrumentation inst) {
        System.out.println("Dreamcatcher agentmain (" + args + ").");
        initialize(args, inst);
    }

    private static void initialize(String args, Instrumentation inst) {
        Dreamcatcher dreamcatcher = new Dreamcatcher(inst, args);
        dreamcatchers.add(dreamcatcher);
    }

    @Override
    public void onChange(File file) {
        if (file.getName().endsWith(".class")) {
            redefineClass(file);
        }
    }

    protected void redefineClass(File classFile) {
        if (!System.getProperties().containsKey(NOCTURNE_UNUSED_RELOADING_CLASS_LOADERS)) {
            System.getProperties().put(NOCTURNE_UNUSED_RELOADING_CLASS_LOADERS, new HashSet<ClassLoader>());
        }

        //noinspection unchecked
        Set<ClassLoader> unusedReloadingClassLoaders
                = (Set<ClassLoader>) System.getProperties().get(NOCTURNE_UNUSED_RELOADING_CLASS_LOADERS);

        // System.out.println("Redefining " + classFile);
        String name = classFile.getAbsolutePath();
        if (name.endsWith(".class")) {
            name = name.substring(0, name.length() - ".class".length());
            String[] tokens = name.split("\\" + File.separator);
            boolean foundClasses = false;
            StringBuilder className = new StringBuilder();
            for (String token : tokens) {
                if (token.equals("classes")) {
                    foundClasses = true;
                } else {
                    if (foundClasses) {
                        className.append(token).append(".");
                    }
                }
            }
            name = className.toString().substring(0, className.length() - 1);
        } else {
            return;
        }

        // System.out.println("Class file name is: `" + name + "`");

        Class[] loadedClasses = inst.getAllLoadedClasses();
        for (Class<?> clazz : loadedClasses) {
            if (unusedReloadingClassLoaders.contains(clazz.getClassLoader())) {
                continue;
            }

            if (clazz.getName().equals(name)) {
                try {
                    // System.out.println("= Ready to redefine  " + clazz.getName() + "@" + clazz.getClassLoader() + " with " + classFile);
                    ClassDefinition definition = new ClassDefinition(clazz, toByteArray(new FileInputStream(classFile)));
                    inst.redefineClasses(new ClassDefinition[]{definition});
                    System.out.println("Redefined " + clazz.getName() + " with " + classFile);
                } catch (Exception e) {
                    System.out.println("Can't redefine " + clazz);
                    System.setProperty("bond.can-not-redefine", "true");
                }
            }
        }
    }


    private static byte[] toByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] buffer = new byte[65536];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            if (bytesRead > 0) {
                baos.write(buffer, 0, bytesRead);
            }
        }

        is.close();
        return baos.toByteArray();
    }

}
