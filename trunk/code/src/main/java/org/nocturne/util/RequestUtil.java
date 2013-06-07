/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.util;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.nocturne.collection.SingleEntryList;
import org.nocturne.exception.NocturneException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Request utilities.
 *
 * @author Mike Mirzayanov
 */
public class RequestUtil {
    private static final String GET_REQUEST_PARAMS_CACHED_RESULT = "Codeforces::getRequestParamsCachedResult";
    private static final Pattern QUERY_STRING_SPLIT_PATTERN = Pattern.compile("&");

    @SuppressWarnings({"unchecked", "OverlyLongMethod", "OverlyComplexMethod"})
    public static Map<String, List<String>> getRequestParams(HttpServletRequest request) {
        Object cachedResult = request.getAttribute(GET_REQUEST_PARAMS_CACHED_RESULT);
        if (cachedResult instanceof Map) {
            return (Map<String, List<String>>) cachedResult;
        }

        if ("post".equalsIgnoreCase(request.getMethod())) {
            try {
                FileItemFactory factory = new DiskFileItemFactory();
                ServletFileUpload upload = new ServletFileUpload(factory);
                List<FileItem> items = upload.parseRequest(request);
                for (FileItem item : items) {
                    InputStream inputStream = item.getInputStream();
                    byte[] bytes = StreamUtil.getAsByteArray(inputStream);
                    if (bytes != null) {
                        if (item.isFormField()) {
                            request.setAttribute(item.getFieldName(), new String(bytes, "UTF-8"));
                        } else {
                            request.setAttribute(item.getFieldName(), bytes);
                        }
                    }
                    inputStream.close();

                    if (item.getName() != null && !item.getName().isEmpty()) {
                        request.setAttribute(item.getFieldName() + "::name", item.getName());
                    }
                }
            } catch (Exception ignored) {
                // No operations.
            }
        }

        Map<String, List<String>> result = new HashMap<String, List<String>>();
        Map<Object, Object> o = request.getParameterMap();
        for (Map.Entry<Object, Object> e : o.entrySet()) {
            String key = e.getKey().toString();
            Object value = e.getValue();

            List<String> parameters;

            if (value.getClass().isArray()) {
                Object[] values = (Object[]) value;
                int count = values.length;

                parameters = count <= 1 ? new SingleEntryList<String>() : new ArrayList<String>(count);

                for (int index = 0; index < count; ++index) {
                    parameters.add(values[index].toString());
                }
            } else {
                parameters = new SingleEntryList<String>(value.toString());
            }

            result.put(key, parameters);
            if (key.endsWith("[]")) {
                key = key.substring(0, key.length() - 2);
                result.put(key, parameters);
            }
        }

        try {
            Enumeration enumeration = request.getAttributeNames();
            while (enumeration.hasMoreElements()) {
                String name = enumeration.nextElement().toString();
                Object value = request.getAttribute(name);
                if (value != null) {
                    if (value instanceof byte[]) {
                        result.put(name, new SingleEntryList<String>(new String((byte[]) value, "UTF-8")));
                    } else {
                        result.put(name, new SingleEntryList<String>(request.getAttribute(name).toString()));
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new NocturneException("Can't use encoding UTF-8.", e);
        }

        request.setAttribute(GET_REQUEST_PARAMS_CACHED_RESULT, result);
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
            if (query != null && !query.isEmpty()) {
                String[] tokens = QUERY_STRING_SPLIT_PATTERN.split(query);
                for (String token : tokens) {
                    if (!token.isEmpty()) {
                        int index = token.indexOf('=');

                        String key;
                        String value;

                        if (index >= 0) {
                            key = token.substring(0, index);
                            value = token.substring(index + 1);
                        } else {
                            key = token;
                            value = "";
                        }

                        try {
                            parameters.put(key, URLDecoder.decode(value, "UTF-8"));
                        } catch (IllegalArgumentException ignored) {
                            // No operations.
                        }
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new NocturneException("Can't decode parameter because of illegal encoding.", e);
        }

        return parameters;
    }

    public static String getFirst(
            @Nonnull Map<String, List<String>> parameterValuesByName, @Nonnull String parameterName) {
        List<String> parameterValues = parameterValuesByName.get(parameterName);
        return parameterValues == null || parameterValues.isEmpty() ? null : parameterValues.get(0);
    }

    public static String getFirst(@Nullable List<String> parameterValues) {
        return parameterValues == null || parameterValues.isEmpty() ? null : parameterValues.get(0);
    }
}
