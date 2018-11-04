package org.nocturne.template;

import java.io.IOException;

/**
 * @author MikeMirzayanov (mirzayanovmr@gmail.com)
 */
public interface TemplatePreprocessor {
    void preprocess(Object source, StringBuilder text) throws IOException;
}
