/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.util;

import org.nocturne.exception.NocturneException;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Enumeration;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;
import java.io.InputStream;

/**
 * Request utilities.
 *
 * @author Mike Mirzayanov
 */
public class RequestUtil {
    public static Map<String, String> getRequestParams(HttpServletRequest request) {
        if (request.getMethod().equalsIgnoreCase("post")) {
            try {
                FileItemFactory factory = new DiskFileItemFactory();
                ServletFileUpload upload = new ServletFileUpload(factory);
                List<FileItem> items = upload.parseRequest(request);
                for (FileItem item : items) {
                    InputStream inputStream = item.getInputStream();
                    byte[] bytes = StreamUtil.getAsByteArray(inputStream);
                    if (bytes != null) {
                        request.setAttribute(item.getFieldName(), bytes);
                    }
                    inputStream.close();
                }
            } catch (Exception e) {
                // No operations.
            }
        }

        Map<String, String> result = new HashMap<String, String>();
        Map<Object, Object> o = request.getParameterMap();
        for (Map.Entry<Object, Object> e : o.entrySet()) {
            String key = e.getKey().toString();
            Object value = e.getValue();
            if (value.getClass().isArray()) {
                Object[] array = (Object[]) value;
                if (array.length == 1) {
                    value = array[0];
                }
            }
            result.put(key, value.toString());
        }

        Enumeration enumeration = request.getAttributeNames();
        while (enumeration.hasMoreElements()) {
            String name = enumeration.nextElement().toString();
            if (request.getAttribute(name) != null) {
                result.put(name, request.getAttribute(name).toString());
            }
        }

        return result;
    }

    /**
     * Parses query part from URL and extracts params.
     *
     * @param request Http request.
     * @return Map containing all parameters as strings. Expects UTF-8 encoding.
     */
    public static Map<String, String> parseGetParameters(HttpServletRequest request) {
        Map<String, String> parameters = new HashMap<String, String>();
        try {
            String query = request.getQueryString();
            if (query != null && query.length() > 0) {
                String[] tokens = query.split("&");
                for (String token : tokens) {
                    if (token.length() > 0) {
                        int index = token.indexOf('=');
                        String key = token.substring(0, index);
                        String value = token.substring(index + 1);
                        try {
                            parameters.put(key, URLDecoder.decode(value, "UTF-8"));
                        } catch (IllegalArgumentException e) {
                            // No operations.
                        }
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new NocturneException("Can't decode parameter bacause of illegal encoding.", e);
        }

        return parameters;
    }
}
