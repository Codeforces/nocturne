/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.main;

import eu.medsea.util.MimeUtil;
import org.apache.log4j.Logger;
import org.nocturne.exception.NocturneException;
import org.nocturne.exception.ReflectionException;
import org.nocturne.module.Module;
import org.nocturne.util.FileUtil;
import org.nocturne.util.ReflectionUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.List;

/**
 * You may use this filter only for debug purpose.
 * Use it for css, js and image resources. It loads
 * resources from modules and if you change
 * resources in IDE it will load renewed version.
 */
public class DebugResourceFilter implements Filter {
    /**
     * Logger.
     */
    private static final Logger logger = Logger.getLogger(DebugResourceFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No operations.
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (ReloadingContext.getInstance().isDebug()) {
            DispatchFilter.updateRequestDispatcher();

            if (getClass().getClassLoader() == DispatchFilter.lastReloadingClassLoader) {
                handleDebugModeDoFilter(request, response, chain);
            } else {
                Object object;

                try {
                    object = DispatchFilter.lastReloadingClassLoader.loadClass(DebugResourceFilter.class.getName()).getConstructor().newInstance();
                } catch (Exception e) {
                    logger.error("Can't create instance of DebugResourceFilter.", e);
                    throw new NocturneException("Can't create instance of DebugResourceFilter.", e);
                }

                try {
                    ReflectionUtil.invoke(object, "handleDebugModeDoFilter", request, response, chain);
                } catch (ReflectionException e) {
                    logger.error("Can't run DebugResourceFilter.", e);
                    throw new NocturneException("Can't run DebugResourceFilter.", e);
                }
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    private static void handleDebugModeDoFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String path = httpRequest.getServletPath();

            List<Module> modules = ApplicationContext.getInstance().getModules();
            for (Module module : modules) {
                if (processModuleResource(module, path, response)) {
                    return;
                }
            }

            String resourcesDir = ApplicationContext.getInstance().getDebugWebResourcesDir();
            if (resourcesDir != null) {
                File resourceFile = new File(resourcesDir, path);
                if (resourceFile.isFile()) {
                    InputStream resourceInputStream = new FileInputStream(resourceFile);
                    writeResourceByPathAndStream(response, path, resourceInputStream);
                    return;
                }
            }

            filterChain.doFilter(request, response);
        }
    }

    private static boolean processModuleResource(Module module, String path, ServletResponse response) throws IOException {
        InputStream inputStream = module.getResourceLoader().getResourceInputStream(path);
        return writeResourceByPathAndStream(response, path, inputStream);
    }

    private static boolean writeResourceByPathAndStream(ServletResponse response, String path, InputStream inputStream) throws IOException {
        if (inputStream != null) {
            OutputStream outputStream = response.getOutputStream();

            try {
                setupContentType(path, response);

                int size = 0;
                byte buffer[] = new byte[65536];

                while (true) {
                    int readCount = inputStream.read(buffer);

                    if (readCount >= 0) {
                        outputStream.write(buffer, 0, readCount);
                        size += readCount;
                    } else {
                        break;
                    }
                }
                response.setContentLength(size);
            } finally {
                outputStream.close();
                inputStream.close();
            }

            return true;
        } else {
            return false;
        }
    }

    private static void setupContentType(String path, ServletResponse response) {
        String type = MimeUtil.getFirstMimeType(MimeUtil.getMimeType(FileUtil.getExt(new File(path))));

        if (type != null) {
            response.setContentType(type);
            return;
        }

        throw new org.nocturne.exception.ServletException("Can't set content type for " + path + '.');
    }

    @Override
    public void destroy() {
    }
}
