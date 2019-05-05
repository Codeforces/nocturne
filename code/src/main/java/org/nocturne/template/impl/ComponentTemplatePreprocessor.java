package org.nocturne.template.impl;

import org.nocturne.main.ApplicationContext;
import org.nocturne.template.TemplatePreprocessor;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author MikeMirzayanov (mirzayanovmr@gmail.com)
 */
public class ComponentTemplatePreprocessor implements TemplatePreprocessor {
    private static final String TAG_TEMPLATE_OPEN = "<template";
    private static final String TAG_TEMPLATE_CLOSE = "</template>";
    private static final String TAG_STYLE_OPEN = "<style";
    private static final String TAG_STYLE_CLOSE = "</style>";
    private static final String TAG_SCRIPT_OPEN = "<script";
    private static final String TAG_SCRIPT_CLOSE = "</script>";

    @Override
    public void preprocess(Object source, StringBuilder text) throws IOException {
        int templateOpenTag = ignoreCaseIndexOf(text, TAG_TEMPLATE_OPEN, true);
        if (templateOpenTag < 0) {
            return;
        }
        if (templateOpenTag == Integer.MAX_VALUE) {
            throw new IOException("Expected at most one <template> tag [" + source + "].");
        }

        int templateCloseTag = ignoreCaseIndexOf(text, TAG_TEMPLATE_CLOSE, false);
        if (templateCloseTag < 0 || templateCloseTag == Integer.MAX_VALUE || templateOpenTag >= templateCloseTag) {
            throw new IOException("Expected exactly one </template> tag, it should be after <template> [" + source + "].");
        }

        int scriptOpenTag = ignoreCaseIndexOf(text, TAG_SCRIPT_OPEN, true);
        if (scriptOpenTag == Integer.MAX_VALUE || (scriptOpenTag != -1 && scriptOpenTag <= templateCloseTag)) {
            throw new IOException("Expected at most one <script> tag, it should be after </template> [" + source + "].");
        }

        int scriptCloseTag = ignoreCaseIndexOf(text, TAG_SCRIPT_CLOSE, false);
        if (scriptCloseTag == Integer.MAX_VALUE || (scriptCloseTag != -1 && scriptOpenTag >= scriptCloseTag)) {
            throw new IOException("Expected exactly one </script> tag, it should be after <script> [" + source + "].");
        }

        if ((scriptOpenTag == -1) != (scriptCloseTag == -1)) {
            throw new IOException("Expected <script> and </script> tags or none of them [" + source + "].");
        }

        int scriptOrTemplateCloseTag = Math.max(scriptCloseTag, templateCloseTag);

        int styleOpenTag = ignoreCaseIndexOf(text, TAG_STYLE_OPEN, true);
        if (styleOpenTag == Integer.MAX_VALUE | (styleOpenTag != -1 && styleOpenTag <= scriptOrTemplateCloseTag)) {
            throw new IOException("Expected at most one <style> tag, it should be after </template> and </script> [" + source + "].");
        }

        int styleCloseTag = ignoreCaseIndexOf(text, TAG_STYLE_CLOSE, false);
        if (styleCloseTag == Integer.MAX_VALUE || (styleCloseTag != -1 && styleOpenTag >= styleCloseTag)) {
            throw new IOException("Expected exactly one </style> tag, it should be after <style> [" + source + "].");
        }

        if ((styleOpenTag == -1) != (styleCloseTag == -1)) {
            throw new IOException("Expected <style> and </style> tags or none of them [" + source + "].");
        }

        Set<String> classes = new HashSet<>();
        preprocessTemplate(source, text, templateOpenTag, classes);
        processComponentClass(source, text, templateOpenTag, classes);

        if (scriptOpenTag != -1) {
            preprocessScript(source, text);
        }

        if (styleOpenTag != -1) {
            preprocessStyle(source, text);
        }
    }

    private void preprocessStyle(Object source, StringBuilder text) throws IOException {
        int styleOpenTag = ignoreCaseIndexOf(text, TAG_STYLE_OPEN, true);
        if (styleOpenTag == -1 || styleOpenTag == Integer.MAX_VALUE) {
            throw new IOException("Expected exactly one <style> tag [" + source + "].");
        }

        int styleCloseTag = ignoreCaseIndexOf(text, TAG_STYLE_CLOSE, false);
        if (styleCloseTag == -1 || styleCloseTag == Integer.MAX_VALUE) {
            throw new IOException("Expected exactly one </style> tag [" + source + "].");
        }

        int from = styleOpenTag;
        while (from < text.length() && text.charAt(from) != '>') {
            from++;
        }

        String textLessAttr = "type=\"text/less\"";
        String textCssAttr = "type=\"text/css\"";
        int less = ignoreCaseIndexOf(text, textLessAttr, true);
        if (less == -1) {
            return;
        } else if (less == Integer.MAX_VALUE) {
            throw new IOException("Expected at most one attribute type=\"text/less\" [" + source + "].");
        }

        if (less > styleOpenTag && less < from && from < text.length()) {
            String css = text.substring(from + 1, styleCloseTag);
            css = Less.compile(source, css, ApplicationContext.getInstance().getComponentTemplatesLessCommonsFile());
            css = css.replaceAll("/\\*[^*]+\\*/", "").trim();
            text.replace(from + 1, styleCloseTag, "\n" + css + "\n");
            text.replace(less, less + textLessAttr.length(), textCssAttr);
        }
    }

    private void preprocessTemplate(Object source, StringBuilder text, int templateOpenTag, Set<String> classes) throws IOException {
        int templateCloseTag = ignoreCaseIndexOf(text, TAG_TEMPLATE_CLOSE, false);
        if (templateCloseTag < 0 || templateCloseTag == Integer.MAX_VALUE || templateOpenTag >= templateCloseTag) {
            throw new IOException("Expected exactly one </template> tag, it should be after <template> [" + source + "].");
        }
        int end = templateCloseTag + TAG_TEMPLATE_CLOSE.length();
        while (end < text.length() && Character.isWhitespace(text.charAt(end))) {
            end++;
        }

        if (end < text.length()) {
            text.insert(end, "<@once>\n");
            int last = text.length() - 1;
            while (last >= 0 && Character.isWhitespace(text.charAt(last))) {
                last--;
            }
            text.insert(last + 1, "\n</@once>");
        }

        String replacement = "<#-- <template name=\"" + getComponentClassName(source) + "\"> -->";
        int templateOpenTagEnd = templateOpenTag;
        while (templateOpenTagEnd < text.length() && text.charAt(templateOpenTagEnd) != '>') {
            templateOpenTagEnd++;
        }
        if (templateOpenTagEnd < text.length()) {
            text.replace(templateOpenTag, templateOpenTagEnd + 1, replacement);
        }

        replacement = "<#-- </template name=\"" + getComponentClassName(source) + "\"> -->";
        templateCloseTag = ignoreCaseIndexOf(text, TAG_TEMPLATE_CLOSE, false);
        if (templateCloseTag < 0 || templateCloseTag == Integer.MAX_VALUE || templateOpenTag >= templateCloseTag) {
            throw new IOException("Expected exactly one </template> tag, it should be after <template> [" + source + "].");
        }
        text.replace(templateCloseTag, templateCloseTag + TAG_TEMPLATE_CLOSE.length(), replacement);

        for (int i = templateOpenTag; i + 8 < templateCloseTag; i++) {
            if ((i == 0 || Character.isWhitespace(text.charAt(i - 1)))
                    && text.charAt(i) == 'c' && text.charAt(i + 1) == 'l' && text.charAt(i + 2) == 'a'
                    && (text.charAt(i + 3) == 's' && text.charAt(i + 4) == 's' || text.charAt(i + 3) == 'z' && text.charAt(i + 4) == 'z')
                    && (text.charAt(i + 5) == '=' || Character.isWhitespace(text.charAt(i + 5)))) {
                int j = i + 5;
                while (j < templateCloseTag && Character.isWhitespace(text.charAt(j))) {
                    j++;
                }
                if (j < templateCloseTag && text.charAt(j) == '=') {
                    j++;
                    while (j < templateCloseTag && Character.isWhitespace(text.charAt(j))) {
                        j++;
                    }
                    if (j < templateCloseTag) {
                        if (text.charAt(j) == '\"' || text.charAt(j) == '\'') {
                            char quote = text.charAt(j);
                            j++;
                            int from = j;
                            while (j < templateCloseTag && text.charAt(j) != quote) {
                                j++;
                            }
                            if (j < templateCloseTag) {
                                addClasses(classes, text, from, j);
                            }
                        } else if (isCssClassPart(text.charAt(j))) {
                            int from = j;
                            while (j < templateCloseTag && isCssClassPart(text.charAt(j))) {
                                j++;
                            }
                            addClasses(classes, text, from, j);
                        }
                    }
                }
            }
        }
    }

    private void addClasses(Set<String> classes, StringBuilder text, int from, int to) {
        for (String s : text.substring(from, to).split("\\s+")) {
            if (s.length() >= 2 && s.charAt(0) == '_' && Character.isLetter(s.charAt(1))) {
                classes.add(s);
            }
        }
    }

    private void preprocessScript(Object source, StringBuilder text) throws IOException {
        int scriptOpenTag = ignoreCaseIndexOf(text, TAG_SCRIPT_OPEN, true);
        int scriptOpenTagEnd = text.indexOf(">", scriptOpenTag);
        if (scriptOpenTagEnd < 0) {
            throw new IOException("Something wrong with <script> tag [" + source + "].");
        }

        text.insert(scriptOpenTagEnd + 1, " $(function () {");

        int scriptCloseTag = ignoreCaseIndexOf(text, TAG_SCRIPT_CLOSE, false);
        if (scriptCloseTag == -1 || scriptCloseTag == Integer.MAX_VALUE) {
            throw new IOException("Something wrong with </script> tag [" + source + "].");
        }

        text.insert(scriptCloseTag, "}); ");
    }

    private String getComponentClassName(Object source) {
        if (source != null) {
            String name = source.toString();
            if (name.endsWith(".ftl")) {
                int index = name.length() - 4;
                while (index >= 0 && name.charAt(index) != '/' && name.charAt(index) != '\\') {
                    index--;
                }
                return "_" + name.substring(index + 1, name.length() - 4);
            }
        }

        return "_Component";
    }

    private void processComponentClass(Object source, StringBuilder sb, int start, Set<String> classes) {
        int index = start;
        while (index + 1 < sb.length()) {
            if ((index == 0 || isComponentClassDelimiter(sb.charAt(index - 1)))
                    && sb.charAt(index) == '_'
                    && (index + 1 < sb.length() && Character.isLetter(sb.charAt(index + 1)))
                    ) {
                int length = 1;
                while (index + length < sb.length() && isCssClassPart(sb.charAt(index + length))) {
                    length++;
                }
                if (length >= 2
                        && (index + length == sb.length() || isComponentClassDelimiter(sb.charAt(index + length)))
                        && classes.contains(sb.substring(index, index + length))) {
                    sb.replace(index, index + length, getComponentClassName(source)
                            + "_" + sb.substring(index + 1, index + length));
                }
            }

            index++;
        }
    }

    private boolean isCssClassPart(char c) {
        return Character.isJavaIdentifierPart(c) || c == '-';
    }

    private boolean isComponentClassDelimiter(char c) {
        return c == ' ' || c == '\"' || c == '\'' || c == '.' || c == '>' || c == '+';
    }

    private int ignoreCaseIndexOf(StringBuilder text, String substring, boolean openTag) {
        if (text.length() < substring.length()) {
            return -1;
        }

        substring = substring.toLowerCase();
        int result = -1;

        for (int i = 0; i <= text.length() - substring.length(); i++) {
            boolean find = true;
            for (int j = 0; j < substring.length(); j++) {
                if (Character.toLowerCase(text.charAt(i + j)) != substring.charAt(j)) {
                    find = false;
                    break;
                }
            }

            if (find && openTag) {
                if (i + substring.length() == text.length()) {
                    find = false;
                } else {
                    char last = text.charAt(i + substring.length());
                    if (last != '>' && !Character.isWhitespace(last)) {
                        find = false;
                    }
                }
            }

            if (find) {
                if (result == -1) {
                    result = i;
                } else {
                    return Integer.MAX_VALUE;
                }
            }
        }

        return result;
    }
}
