/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.main;

import org.nocturne.link.LinkMatchResult;
import org.nocturne.link.Links;
import org.nocturne.util.RequestUtil;
import org.nocturne.util.StringUtil;

import java.util.List;
import java.util.Map;

/**
 * @author Mike Mirzayanov
 */
public class LinkedRequestRouter implements RequestRouter {
    @Override
    public Resolution route(String path, Map<String, List<String>> parameterMap) {
        String action = RequestUtil.getFirst(parameterMap, "action");
        LinkMatchResult linkMatchResult = Links.match(path);

        if (linkMatchResult != null) {
            ApplicationContext.getInstance().setLink(linkMatchResult.getLink());

            Map<String, String> attrs = linkMatchResult.getAttributes();
            if (attrs != null) {
                for (Map.Entry<String, String> entry : attrs.entrySet()) {
                    ApplicationContext.getInstance().getRequest().setAttribute(
                            ApplicationContext.getAdditionalParamsRequestAttributePrefix() + entry.getKey(), entry.getValue()
                    );
                    if ("action".equals(entry.getKey())) {
                        action = entry.getValue();
                    }
                }
            }

            if (StringUtil.isEmptyOrNull(action)) {
                action = linkMatchResult.getLink().action();
            }

            if (action == null) {
                action = "";
            }

            Resolution resolution = new Resolution(linkMatchResult.getPageClass().getName(), action);
            if (attrs != null) {
                for (Map.Entry<String, String> entry : attrs.entrySet()) {
                    resolution.addOverrideParameter(entry.getKey(), entry.getValue());
                }
            }

            return resolution;
        } else {
            return null;
        }
    }
}
