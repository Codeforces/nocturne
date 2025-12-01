/*
 * Copyright by Mike Mirzayanov
 */
package bloggy;

import bloggy.database.ApplicationDataSource;
import org.jacuzzi.core.Jacuzzi;

import javax.servlet.*;
import java.io.IOException;

/**
 * @author Mike Mirzayanov (mirzayanovmr@gmail.com)
 */
public class PreprocessFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) {
        // No operations.
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        Jacuzzi jacuzzi = Jacuzzi.getJacuzzi(ApplicationDataSource.getInstance());
        try {
            jacuzzi.attachConnection();
            chain.doFilter(request, response);
        } finally {
            jacuzzi.detachConnectionOrThrowException();
        }
    }

    @Override
    public void destroy() {
        // No operations.
    }
}
