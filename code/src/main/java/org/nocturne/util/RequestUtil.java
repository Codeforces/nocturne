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
import java.nio.charset.StandardCharsets;
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

    public static String getRequestUriAndQueryString(HttpServletRequest request) {
        String result = request.getRequestURI();
        if (StringUtil.isNotEmpty(request.getQueryString())) {
            result += "?" + request.getQueryString();
        }
        return result;
    }

    public static Map<String, List<String>> getRequestParams(HttpServletRequest request) {
        Map<String, List<String>> cachedRequestParameters = getCachedRequestParameters(request);
        if (cachedRequestParameters != null) {
            return cachedRequestParameters;
        }

        if ("POST".equalsIgnoreCase(request.getMethod())) {
            addUploadedItemsToRequestAttributes(request);
        }

        Map<String, List<String>> requestParameters = new HashMap<>();

        addRequestParametersFromParameterMap(requestParameters, request);
        addRequestParametersFromAttributes(requestParameters, request);

        setCachedRequestParameters(request, requestParameters);

        return requestParameters;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<String>> getCachedRequestParameters(HttpServletRequest request) {
        Object cachedRequestParameters = request.getAttribute(GET_REQUEST_PARAMS_CACHED_RESULT);
        if (cachedRequestParameters instanceof Map) {
            return (Map<String, List<String>>) cachedRequestParameters;
        } else {
            return null;
        }
    }

    private static void setCachedRequestParameters(
            HttpServletRequest request, Map<String, List<String>> requestParameters) {
        request.setAttribute(GET_REQUEST_PARAMS_CACHED_RESULT, requestParameters);
    }

    private static void addUploadedItemsToRequestAttributes(HttpServletRequest request) {
        try {
            FileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);
            List<FileItem> items = upload.parseRequest(request);

            Map<String, List<String>> fileNamesByFieldName = new HashMap<>();
            Map<String, List<byte[]>> fileBytesByFieldName = new HashMap<>();

            for (FileItem item : items) {
                String name = item.getFieldName();
                InputStream inputStream = item.getInputStream();
                byte[] bytes = StreamUtil.getAsByteArray(inputStream);
                if (bytes != null) {
                    if (item.isFormField()) {
                        Object existingValue = request.getAttribute(name);
                        String value = new String(bytes, StandardCharsets.UTF_8);

                        if (existingValue == null) {
                            request.setAttribute(name, value);
                        } else if (existingValue instanceof Collection) {
                            addStringToRawCollection((Collection) existingValue, value);
                        } else {
                            Collection<Object> values = new ArrayList<>(4);
                            values.add(existingValue);
                            values.add(value);
                            request.setAttribute(name, values);
                        }
                    } else {
                        request.setAttribute(name, bytes);

                        if (!fileBytesByFieldName.containsKey(name)) {
                            fileBytesByFieldName.put(name, new ArrayList<byte[]>(1));
                        }
                        fileBytesByFieldName.get(name).add(bytes);
                    }
                }
                inputStream.close();

                if (item.getName() != null && !item.getName().isEmpty()) {
                    request.setAttribute(name + "::name", item.getName());

                    if (!fileNamesByFieldName.containsKey(name)) {
                        fileNamesByFieldName.put(name, new ArrayList<String>(1));
                    }
                    fileNamesByFieldName.get(name).add(item.getName());
                }
            }


            for (Map.Entry<String, List<String>> e : fileNamesByFieldName.entrySet()) {
                String fieldName = e.getKey();

                String[] fileNames = new String[e.getValue().size()];
                e.getValue().toArray(fileNames);
                request.setAttribute(fieldName + "::name[]", fileNames);

                byte[][] fileBytes = new byte[e.getValue().size()][];
                fileBytesByFieldName.get(fieldName).toArray(fileBytes);
                request.setAttribute(fieldName + "[]", fileBytes);
            }
        } catch (Exception ignored) {
            // No operations.
        }
    }

    private static void addRequestParametersFromParameterMap(
            Map<String, List<String>> requestParameters, HttpServletRequest request) {
        Map<?, ?> parameterMap = request.getParameterMap();

        for (Map.Entry<?, ?> e : parameterMap.entrySet()) {
            String name = e.getKey().toString();
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
                parameters = new SingleEntryList<>(value.toString());
            }

            requestParameters.put(name, parameters);

            if (name.endsWith("[]")) {
                name = name.substring(0, name.length() - "[]".length());
                requestParameters.put(name, parameters);
            }
        }
    }

    private static void addRequestParametersFromAttributes(
            Map<String, List<String>> requestParameters, HttpServletRequest request) {
        Enumeration enumeration = request.getAttributeNames();

        while (enumeration.hasMoreElements()) {
            String name = enumeration.nextElement().toString();
            Object value = request.getAttribute(name);

            if (value != null) {
                if (value instanceof byte[]) {
                    requestParameters.put(name, new SingleEntryList<>(new String((byte[]) value, StandardCharsets.UTF_8)));
                    continue;
                }

                if (value instanceof List) {
                    List list = (List) value;
                    if (isListOfStrings(list)) {
                        requestParameters.put(name, castToStringList(list));
                        continue;
                    }
                }

                if (value instanceof Collection) {
                    List<?> list = new ArrayList<>((Collection<?>) value);
                    if (isListOfStrings(list)) {
                        requestParameters.put(name, castToStringList(list));
                        continue;
                    }
                }

                requestParameters.put(name, new SingleEntryList<>(request.getAttribute(name).toString()));
            }
        }
    }

    private static boolean isListOfStrings(List list) {
        for (Object o : list) {
            if (!(o instanceof String)) {
                return false;
            }
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private static List<String> castToStringList(List list) {
        return (List<String>) list;
    }

    @SuppressWarnings("unchecked")
    private static void addStringToRawCollection(Collection existingValue, String value) {
        existingValue.add(value);
    }

    /**
     * Parses query part from URL and extracts params.
     *
     * @param request Http request.
     * @return Map containing all parameters as strings. Expects UTF-8 encoding.
     */
    public static Map<String, String> parseGetParameters(HttpServletRequest request) {
        Map<String, String> parameters = new HashMap<>();
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
                            parameters.put(key, URLDecoder.decode(value, StandardCharsets.UTF_8.name()));
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
