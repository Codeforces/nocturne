package org.nocturne.misc;

/** @author Mike Mirzayanov */
public class ClassMatcher {
    private String s;
    private int cursor;
    private String clazz;

    private boolean matchPrefix(String prefix) {
        return clazz.equals(prefix) || clazz.startsWith(prefix + ".") || clazz.startsWith(prefix + "$");
    }

    private boolean matchCondition(String condition) {
        if (condition.charAt(0) == '+') {
            return matchPrefix(condition.substring(1));
        } else {
            return !matchPrefix(condition.substring(1));
        }
    }

    public ClassMatcher(String s) {
        this.s = s.trim();
    }

    public synchronized boolean match(String clazz) {
        cursor = 0;
        this.clazz = clazz;
        boolean result = readOr();
        if (cursor < s.length()) {
            throw new IllegalArgumentException("Class pattern is invalid: " + s);
        }
        return result;
    }

    private boolean readOr() {
        boolean lf = readXor();

        while (cursor < s.length() && s.charAt(cursor) == 'O') {
            if (s.charAt(cursor + 1) != 'R') {
                throw new IllegalArgumentException("Invalid expression: OR expected.");
            }
            cursor += 2;
            boolean rg = readXor();
            lf = lf || rg;
        }

        return lf;
    }

    private boolean readXor() {
        boolean lf = readAnd();

        while (cursor < s.length() && s.charAt(cursor) == '^') {
            cursor += 1;
            boolean rg = readAnd();
            lf = lf ^ rg;
        }

        return lf;
    }

    private boolean readAnd() {
        boolean lf = readUnary();

        while (cursor < s.length() && s.charAt(cursor) == 'A') {
            if (s.charAt(cursor + 1) != 'N' || s.charAt(cursor + 2) != 'D') {
                throw new IllegalArgumentException("Invalid expression: AND expected.");
            }

            cursor += 3;
            boolean rg = readUnary();
            lf = lf && rg;
        }

        return lf;
    }

    private boolean readUnary() {
        try {
            while (cursor < s.length() && s.charAt(cursor) <= ' ') {
                cursor++;
            }

            if (s.charAt(cursor) == '+' || s.charAt(cursor) == '-') {
                StringBuffer sb = new StringBuffer();
                while (cursor < s.length() && isConditionCharacter(s.charAt(cursor))) {
                    sb.append(s.charAt(cursor++));
                }
                return matchCondition(sb.toString());
            }

            if (s.charAt(cursor) == '!') {
                cursor++;
                return !readUnary();
            }

            if (s.charAt(cursor) == '(') {
                cursor++;
                boolean result = readOr();
                cursor++;
                return result;
            }
        } finally {
            while (cursor < s.length() && s.charAt(cursor) <= ' ') {
                cursor++;
            }
        }

        throw new IllegalStateException("Unknown unary " + s.charAt(cursor));
    }

    private static boolean isConditionCharacter(char c) {
        return Character.isJavaIdentifierPart(c) || c == '+' || c == '-' || c == '.';
    }
}
