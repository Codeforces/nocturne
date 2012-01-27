/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.module;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileResourceLoader implements ResourceLoader {
    private final File baseDir;

    public FileResourceLoader(File baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public InputStream getResourceInputStream(String path) throws IOException {
        File file = new File(baseDir, path);

        if (file.isFile()) {
            return new FileInputStream(file);
        } else {
            return null;
        }
    }
}
