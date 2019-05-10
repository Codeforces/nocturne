package bloggy;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;

/**
 * @author Mike Mirzayanov
 */
public class NginxFilter implements Filter {
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";
    private static final String X_REAL_IP = "X-Real-IP";

    @Override
    public void init(FilterConfig filterConfig) {
        // No operations.
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;

            String ip = httpRequest.getHeader(X_REAL_IP);
            String host = httpRequest.getHeader(X_FORWARDED_FOR);

            String proto = httpRequest.getHeader(X_FORWARDED_PROTO);
            String tmpRequestUrl = httpRequest.getRequestURL().toString();
            if (tmpRequestUrl.startsWith("http://") && "https".equalsIgnoreCase(proto)) {
                tmpRequestUrl = "https://" + tmpRequestUrl.substring(7);
            }

            final String requestURL = tmpRequestUrl;
            filterChain.doFilter(new HttpServletRequestWrapper(httpRequest) {
                @Override
                public String getRemoteAddr() {
                    return ip != null ? ip : super.getRemoteAddr();
                }

                @Override
                public String getRemoteHost() {
                    return host != null ? host : super.getRemoteHost();
                }

                @Override
                public StringBuffer getRequestURL() {
                    return new StringBuffer(requestURL);
                }
            }, response);
        }
    }

    @Override
    public void destroy() {
        // No operations.
    }
}
