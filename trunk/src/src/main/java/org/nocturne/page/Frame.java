package org.nocturne.page;

import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.nocturne.exception.FreemarkerException;

/** @author Mike Mirzayanov */
public abstract class Frame extends Component {
    String parseTemplate() {
        prepareForRender();
        beforeRender();
        render();
        afterRender();

        try {
            StringWriter writer = new StringWriter(128);
            Map<String, Object> params = new HashMap<String, Object>(getTemplateMap());
            params.putAll(ComponentLocator.getCurrentPage().getGlobalTemplateMap());

            getTemplate().process(params, writer);
            return writer.getBuffer().toString();
        } catch (TemplateException e) {
            throw new FreemarkerException("Can't parse frame " + getClass().getSimpleName() + ".", e);
        } catch (IOException e) {
            throw new FreemarkerException("Can't parse frame " + getClass().getSimpleName() + ".", e);
        } finally {
            finalizeAfterRender();
        }
    }
}
