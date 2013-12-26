package ja.agent;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mike Mirzayanov (mirzayanovmr@gmail.com)
 */
public class DirectoryListener {
    private List<Handler> handlers = new ArrayList<>();
    private WatchService watcher;

    /* init. */ {
        try {
            watcher = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw new RuntimeException("Can't create WatchService. Are you sure you are using Java 7+?");
        }
    }

    public void addHandler(Handler handler) {
        handlers.add(handler);
    }

    public void addRootDir(File rootDir) {
        try {
            listenDirectory(rootDir);
            System.out.println("Started to listen directory '" + rootDir + "'.");
        } catch (IOException e) {
            throw new RuntimeException("Can't listen directory '" + rootDir + "'.", e);
        }
    }

    public void start() {
        Thread innerThread = new Thread(null, new DirectoryWatcherRunnable(), "DirectoryListenerThread");
        innerThread.setDaemon(true);
        innerThread.start();
        System.out.println("DirectoryListenerThread has been started.");
    }

    private void listenDirectory(File rootDir) throws IOException {
        Files.walkFileTree(rootDir.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
                    throws IOException {
                directory.register(
                        watcher,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY
                );
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public interface Handler {
        void onChange(File file);
    }

    private class DirectoryWatcherRunnable implements Runnable {
        @Override
        public void run() {
            while (true) {
                WatchKey key;

                try {
                    key = watcher.take();
                } catch (InterruptedException ignored) {
                    System.out.println("DirectoryListenerThread has been interrupted.");
                    break;
                }

                for (WatchEvent<?> watchEvent : key.pollEvents()) {
                    final WatchEvent.Kind<?> kind = watchEvent.kind();
                    if (StandardWatchEventKinds.OVERFLOW == kind) {
                        continue;
                    }

                    //noinspection unchecked
                    final WatchEvent<Path> pathWatchEvent = (WatchEvent<Path>) watchEvent;
                    final Path path = pathWatchEvent.context();
                    final File asFile = new File(key.watchable() + "", path.toString());

                    if (asFile.isDirectory() && kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        System.out.println("Listen new directory '" + path + "'.");
                        try {
                            listenDirectory(asFile);
                        } catch (IOException e) {
                            throw new RuntimeException("Can't listen new directory '" + asFile + "'.", e);
                        }
                    }

                    if (asFile.isFile()) {
                        for (Handler handler : handlers) {
                            handler.onChange(asFile);
                        }
                    }
                }

                if (!key.reset()) {
                    System.out.println("DirectoryListenerThread: !key.reset().");
                    break;
                }
            }

            System.out.println("Finished DirectoryListenerThread.");
        }
    }
}
