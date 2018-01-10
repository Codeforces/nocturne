/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.main;

import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.detector.ExtensionMimeDetector;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.nocturne.exception.NocturneException;
import org.nocturne.exception.ReflectionException;
import org.nocturne.module.Module;
import org.nocturne.util.ReflectionUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * You may use this filter only for debug purpose.
 * Use it for css, js and image resources. It loads
 * resources from modules and if you change
 * resources in IDE it will load renewed version.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class DebugResourceFilter implements Filter {
    private static final Logger logger = Logger.getLogger(DebugResourceFilter.class);

    static {
        String mimeDetectorName = ExtensionMimeDetector.class.getName();
        if (eu.medsea.mimeutil.MimeUtil.getMimeDetector(mimeDetectorName) == null) {
            eu.medsea.mimeutil.MimeUtil.registerMimeDetector(mimeDetectorName);
        }
    }

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
            try (OutputStream outputStream = response.getOutputStream()) {
                setupContentType(path, response);

                int size = 0;
                byte[] buffer = new byte[65536];

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
                inputStream.close();
            }

            return true;
        } else {
            return false;
        }
    }

    @Contract("null, _ -> fail")
    private static void setupContentType(String path, ServletResponse response) {
        if (path != null) {
            String mimeType = MimeUtil.getMimeType(path);

            if (mimeType != null) {
                response.setContentType(mimeType);
                return;
            }
        }

        throw new org.nocturne.exception.ServletException("Can't set content type for " + path + '.');
    }

    @Override
    public void destroy() {
    }

    private static final class MimeUtil {
        private static final Map<String, String> mimeTypeByExtension = new ConcurrentHashMap<>();

        private static void add(String mimeType, String... extensions) {
            for (String extension : extensions) {
                if (mimeTypeByExtension.containsKey(extension)) {
                    throw new NocturneException("Already has registered mime type by " + extension + '.');
                }
                mimeTypeByExtension.put(extension, mimeType);
            }
        }

        private static String getMimeType(String path) {
            String extension = (path.indexOf('.') < 0 ? path : path.substring(path.lastIndexOf('.') + 1)).toLowerCase();
            String result = mimeTypeByExtension.get(extension);
            if (result != null) {
                return result;
            }

            MimeType mimeType = eu.medsea.mimeutil.MimeUtil.getMostSpecificMimeType(eu.medsea.mimeutil.MimeUtil.getMimeTypes(new File(path).getName()));
            if (mimeType != null) {
                return mimeType.toString();
            }

            return "application/octet-stream";
        }

        static {
            add("application/wasm", "wasm");
            add("application/json", "json");
            add("application/javascript", "js");
            add("application/pdf", "pdf");
            add("application/postscript", "ps");
            add("application/font-woff", "woff");
            add("application/xhtml+xml", "xhtml");
            add("application/xml-dtd", "dtd");
            add("application/zip", "zip");
            add("application/gzip", "gzip");
            add("application/x-tex", "tex");
            add("application/xml", "xml");
            add("audio/aac", "acc");
            add("audio/mpeg", "mp3");
            add("audio/ogg", "ogg");
            add("image/gif", "gif");
            add("image/jpeg", "jpeg");
            add("image/png", "png");
            add("image/svg+xml", "svg");
            add("image/tiff", "tiff");
            add("image/webp", "webp");
            add("image/bmp", "bmp");
            add("text/plain", "txt");
            add("text/css", "css");
            add("text/html", "html", "htm");
            add("text/x-java-source", "java");
            add("text/x-c", "cpp");
            add("text/x-c", "c");
            add("video/avi", "avi");
            add("video/mp4", "mp4");
            add("video/mpeg", "mpeg");
        }
    }

}
