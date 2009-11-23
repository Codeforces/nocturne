/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.util;

import org.nocturne.exception.NocturneException;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.HashMap;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;

/**
 * Request utilities.
 *
 * @author Mike Mirzayanov
 */
public class RequestUtil {
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
