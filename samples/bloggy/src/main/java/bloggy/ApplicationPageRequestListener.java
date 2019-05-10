/*
 * Copyright by Mike Mirzayanov
 */
package bloggy;

import com.codeforces.commons.exception.ExceptionUtil;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.log4j.Logger;
import org.nocturne.listener.PageRequestListener;
import org.nocturne.main.Page;
import bloggy.web.page.WebPage;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Mike Mirzayanov (mirzayanovmr@gmail.com)
 */
public class ApplicationPageRequestListener implements PageRequestListener {
    private static final Logger logger = Logger.getLogger(ApplicationPageRequestListener.class);

    @Override
    public void beforeProcessPage(Page page) {
        // No operations.
    }

    private static boolean isClientAbortException(Throwable e) {
        return e != null && (e.getClass().getName().contains("ClientAbortException")
                || (e.getCause() != null && e.getCause().getClass().getName().contains("ClientAbortException")));
    }

    @Override
    public void afterProcessPage(Page page, Throwable t) {
        if (isClientAbortException(t)) {
            t = null;
        }

        if (t != null) {
            logger.error("Error page has been shown while trying to show " + page.getClass().getName() + ' ' +
                    "on exception " + t + '.', t);
            logger.error(ExceptionUtil.toString(t));
            page.getResponse().setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            StringBuilder paramsDump = new StringBuilder();
            addRequestParameters(page, paramsDump);
            addRequestAttributes(page, paramsDump);

            t.printStackTrace();
        }

        if (t != null && page instanceof WebPage) {
            try {
                Template template = page.getTemplateEngineConfiguration().getTemplate("ErrorPage.ftl");

                Map<String, Object> params = new HashMap<>();

                if (page.getTemplateMap() != null) {
                    params.putAll(page.getTemplateMap());
                }

                if (page.getGlobalTemplateMap() != null) {
                    params.putAll(page.getGlobalTemplateMap());
                }

                params.put("error", ExceptionUtil.toString(t));

                template.setOutputEncoding("UTF-8");
                template.process(params, page.getWriter());
            } catch (TemplateException | IOException ignored) {
                // No operations.
            }
        }
    }

    private static void addRequestAttributes(Page page, StringBuilder paramsDump) {
        Enumeration enumeration = page.getRequest().getAttributeNames();
        while (enumeration.hasMoreElements()) {
            String name = enumeration.nextElement().toString();
            Object value = page.getRequest().getAttribute(name);
            addParam(paramsDump, name, value);
        }
    }

    private static void addRequestParameters(Page page, StringBuilder paramsDump) {
        for (Object o : page.getRequest().getParameterMap().entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            String key = entry.getKey().toString();
            Object valueObject = entry.getValue();
            addParam(paramsDump, key, valueObject);
        }
    }

    private static void addParam(StringBuilder paramsDump, String key, Object value) {
        String stringValue = null;

        if (value instanceof byte[]) {
            stringValue = new String((byte[]) value, StandardCharsets.UTF_8);
        }

        if (stringValue == null && value.getClass().isArray() && ((Object[]) value).length == 1) {
            value = getFirstObject((Object[]) value);
        }

        if (value != null) {
            stringValue = value.toString();
        }

        if (stringValue != null && stringValue.length() > 50) {
            stringValue = stringValue.substring(0, 25) + "..."
                    + stringValue.substring(stringValue.length() - 25);
        }

        paramsDump.append("\n        ").append(key).append("=\"").append(stringValue).append('\"');
    }

    private static Object getFirstObject(Object[] value) {
        return value[0];
    }
}
