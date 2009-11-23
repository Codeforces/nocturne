/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.module;

import java.io.IOException;
import java.io.InputStream;

/**
 * Internal interface to get resources as streams by their names.
 *
 * @author Mike Mirzayanov
 */
public interface ResourceLoader {
    InputStream getResourceInputStream(String path) throws IOException;
}
