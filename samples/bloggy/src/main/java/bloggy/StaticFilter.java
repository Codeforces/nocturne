package bloggy;

import com.codeforces.commons.properties.PropertiesUtil;
import com.codeforces.commons.text.StringUtil;
import org.nocturne.main.ApplicationContext;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class StaticFilter implements Filter {
    private static final long CACHE_TIMEOUT = TimeUnit.DAYS.toSeconds(1);
    private static final Pattern STATIC_RESOURCE_URI_PATTERN = Pattern.compile("[/A-Za-z0-9_.]{1,128}");
    private static final boolean ENABLED = true; // ApplicationContext.getInstance().isDebug();
    private static final String STATIC_SRC_PATH = PropertiesUtil.getProperty("debug.static-src-path", "",
            "/application.properties");

    private FilterConfig filterConfig;

    private boolean isResourceRequestUri(String requestUri) {
        return !StringUtil.isBlank(requestUri) && STATIC_RESOURCE_URI_PATTERN.matcher(requestUri).matches()
                && !requestUri.contains("..") && !requestUri.contains("//");
    }

    @Override
    public void init(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (ENABLED && request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            HttpServletResponse httpServletResponse = (HttpServletResponse) response;

            String requestURI = httpServletRequest.getRequestURI();
            if (isResourceRequestUri(requestURI)) {
                File file = new File(STATIC_SRC_PATH, requestURI);
                if (!file.isFile()) {
                    file = new File(filterConfig.getServletContext().getRealPath(requestURI));
                }
                if (file.isFile()) {
                    long now = System.currentTimeMillis() / 1000;

                    httpServletResponse.setHeader("Cache-Control", "public, max-age=" + CACHE_TIMEOUT);
                    httpServletResponse.setHeader("Date", formatTime(now * 1000));
                    httpServletResponse.setHeader("Expires", formatTime(now * 1000 + CACHE_TIMEOUT * 1000));

                    httpServletResponse.setContentLength((int) file.length());
                    httpServletResponse.setContentType(filterConfig.getServletContext().getMimeType(file.getCanonicalPath()));
                    httpServletResponse.setHeader("Pragma", "public");
                    httpServletRequest.setCharacterEncoding(StandardCharsets.UTF_8.name());

                    ServletOutputStream outputStream = httpServletResponse.getOutputStream();
                    try {
                        Files.copy(file.toPath(), outputStream);
                    } finally {
                        outputStream.flush();
                    }
                }
            }
        }

        chain.doFilter(request, response);
    }

    private static String formatTime(long time) {
        SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.UK);
        fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        return fmt.format(time);
    }

    @Override
    public void destroy() {
        // No operations.
    }
}
