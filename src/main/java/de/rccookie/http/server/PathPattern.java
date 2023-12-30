package de.rccookie.http.server;

import java.util.regex.Pattern;

import de.rccookie.util.Arguments;

class PathPattern {

    private final String pattern;
    private final Pattern regex;

    private PathPattern(String pattern) {
        this.pattern = Arguments.checkNull(pattern, "pattern");

        String withoutEscaped = pattern.replace("\\*", (char) 0 + "");
        if(withoutEscaped.contains("***"))
            throw new IllegalArgumentException(pattern+": Illegal sequence of wildcard characters");

        regex = Pattern.compile('^' + withoutEscaped
                .replace("/**", "(/." + (char) 0 + ")?")
                .replace("**/", ".+/")
                .replace("**", "." + (char) 0)
                .replace("*", "[^/]+")
                .replace((char) 0, '*')
                .replace("/", "\\/") + '$');
    }

    public boolean matches(String path) {
        return regex.matcher(path).matches();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PathPattern && pattern.equals(((PathPattern) obj).pattern);
    }

    @Override
    public int hashCode() {
        return pattern.hashCode();
    }

    @Override
    public String toString() {
        return pattern;
    }

    public static PathPattern parse(String pattern) {
        return new PathPattern(pattern);
    }

    public static boolean containsPattern(String pattern) {
        return pattern.replace("\\*", "").contains("*");
    }
}
