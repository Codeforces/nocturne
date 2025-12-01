package org.nocturne.postprocess;

import org.nocturne.main.Page;

/**
 * Use this interface to postprocess ready to render html from the page.
 * To use it, just configure your Google Guice module to bind it to
 * implementation.
 *
 * @author MikeMirzayanov (mirzayanovmr@gmail.com)
 */
public interface ResponsePostprocessor {
    String postprocess(Page page, String postprocess);
}
