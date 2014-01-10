package org.nocturne.dreamcatcher;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Mike Mirzayanov (mirzayanovmr@gmail.com)
 */
public class DirectoryListener {
    private static final Logger logger = Logger.getLogger(DirectoryListener.class);

    private final List<Handler> handlers = new ArrayList<>();
    private final WatchService watcher;

    private final Lock delayedFilesLock = new ReentrantLock();
    private final BlockingQueue<FileAndTimestamp> delayedFilesQueue = new LinkedBlockingQueue<>();
    private final Set<FileAndTimestamp> delayedFilesSet = new HashSet<>();

    DirectoryListener() {
        try {
            watcher = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw new RuntimeException("Can't create WatchService. Are you sure you are using Java 7+?", e);
        }

        Thread delayedFilesQueueListenerThread = new Thread(new DelayedQueueRunnable(), "DirectoryListener.delayedFilesQueueListenerThread");
        delayedFilesQueueListenerThread.setDaemon(true);
        delayedFilesQueueListenerThread.start();
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

    private class DelayedQueueRunnable implements Runnable {
        private static final long MIN_DELAY_MILLIS = 1000;

        @Override
        public void run() {
            while (true) {
                FileAndTimestamp fileAndTimestamp;
                try {
                    fileAndTimestamp = delayedFilesQueue.poll(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    logger.warn("DelayedQueueRunnable interrupted.", e);
                    break;
                }

                if (fileAndTimestamp != null) {
                    long passedTime = System.currentTimeMillis() - fileAndTimestamp.timestamp;
                    if (passedTime < MIN_DELAY_MILLIS) {
                        try {
                            Thread.sleep(MIN_DELAY_MILLIS - passedTime);
                        } catch (InterruptedException e) {
                            logger.warn("DelayedQueueRunnable interrupted.", e);
                            break;
                        }
                    }

                    delayedFilesLock.lock();
                    try {
                        delayedFilesSet.remove(fileAndTimestamp);
                    } finally {
                        delayedFilesLock.unlock();
                    }

                    File file = fileAndTimestamp.file;
                    if (file.isFile()) {
                        System.out.println("File " + file + " with timestamp " + fileAndTimestamp.timestamp + " processed.");
                        for (Handler handler : handlers) {
                            handler.onChange(file);
                        }
                    }
                }
            }
        }
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
                    if (StandardWatchEventKinds.OVERFLOW.equals(kind)) {
                        continue;
                    }

                    //noinspection unchecked
                    final WatchEvent<Path> pathWatchEvent = (WatchEvent<Path>) watchEvent;
                    final Path path = pathWatchEvent.context();
                    final File asFile = new File(String.valueOf(key.watchable()), path.toString());

                    System.out.println("EVENT: " + asFile + " " + kind
                            + " " + asFile.isFile() + " " + asFile.isDirectory() + " " + asFile.exists());

                    if (asFile.isDirectory() && StandardWatchEventKinds.ENTRY_CREATE.equals(kind)) {
                        System.out.println("Listen new directory '" + path + "'.");
                        try {
                            listenDirectory(asFile);
                        } catch (IOException e) {
                            throw new RuntimeException("Can't listen new directory '" + asFile + "'.", e);
                        }
                    }

//                    if (asFile.isFile()) {
//                        System.out.println("IsFile: " + asFile);
//                        for (Handler handler : handlers) {
//                            handler.onChange(asFile);
//                        }
//                    }

                    long currentTimeSeconds = (System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(1) - 1) / TimeUnit.SECONDS.toMillis(1);
                    for (int addSeconds = 0; addSeconds <= 2; ++addSeconds) {
                        FileAndTimestamp fileAndTimestamp = new FileAndTimestamp(asFile, TimeUnit.SECONDS.toMillis(currentTimeSeconds + addSeconds));

                        delayedFilesLock.lock();
                        try {
                            if (!delayedFilesSet.contains(fileAndTimestamp)) {
                                delayedFilesSet.add(fileAndTimestamp);
                                delayedFilesQueue.add(fileAndTimestamp);
                            }
                        } finally {
                            delayedFilesLock.unlock();
                        }
                    }
                }

                key.reset();
            }

            System.out.println("Finished DirectoryListenerThread.");
        }
    }

    private static class FileAndTimestamp {
        private final File file;
        private final long timestamp;

        private FileAndTimestamp(File file, long timestamp) {
            this.file = file;
            this.timestamp = timestamp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FileAndTimestamp that = (FileAndTimestamp) o;

            if (timestamp != that.timestamp) return false;
            if (file != null ? !file.equals(that.file) : that.file != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = file != null ? file.hashCode() : 0;
            result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
            return result;
        }
    }
}
