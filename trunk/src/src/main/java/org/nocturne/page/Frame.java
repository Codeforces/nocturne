package org.nocturne.page;

import freemarker.template.TemplateException;
import org.nocturne.misc.AbortException;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/** @author Mike Mirzayanov */
public abstract class Frame extends Component {
    String parseTemplate() throws IOException {
        try {
            prepareForRender();
            beforeRender();
            render();
            afterRender();

            try {
                StringWriter writer = new StringWriter(128);
                Map<String, Object> params = new HashMap<String, Object>(getTemplateMap());
                params.putAll(ComponentLocator.getPage().getGlobalTemplateMap());

                getTemplate().process(params, writer);
                return writer.getBuffer().toString();
            } catch (TemplateException e) {
                throw new IllegalStateException("Can't parse frame " + getClass().getSimpleName() + ".", e);
            }
        } catch (AbortException e) {
            // No operations.
        }

        // Commented in order to allow abort with redirection from a frame.
        // throw new IllegalStateException("Can't return value.");

        return null;
    }
}
