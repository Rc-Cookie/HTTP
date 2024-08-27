package de.rccookie.http.server;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.rccookie.http.Route;
import de.rccookie.util.Arguments;

class RoutePattern {

    private static final Pattern VAR_PATTERN = Pattern.compile("([^\\\\])<((?:[^>/:]|\\\\>|\\\\/)+)(?::((?:[^>/]|\\\\>|\\\\/)+))?>");

    private final String pattern;
    private final Pattern regex;
    private final List<String> variables;
    private final boolean containsDoubleWildcard;


    private RoutePattern(String pattern) {
        this.pattern = Arguments.checkNull(pattern, "pattern");

        String withPathVars = pattern.replace("\\*", (char) 0 + "");

        variables = new ArrayList<>();
        List<String> innerRegexes = new ArrayList<>();
        String withoutEscaped = VAR_PATTERN.matcher(withPathVars).replaceAll(r -> {
            variables.add(r.group(2).replace("\\/", "/").replace("\\>", ">").replace("\\:", ":"));
            String regex = r.group(3);
            if(regex == null)
                innerRegexes.add("(?<v"+(variables.size()-1)+">[^/]+)");
            else {
                regex = (variables.size()-1)+">"+regex.replace("\\/", "/").replace("\\>", ">");
//                if("".matches(regex) && r.group(1).equals("/") && (r.end() == withPathVars.length() || withPathVars.charAt(r.end()) == '/'))
//                    regex = "()"
                // TODO: Modify regex to allow no path at all (so no leading '/') if above test is true
                innerRegexes.add("(?<v"+regex+")");
            }
            return r.group(1)+(char) 1;
        });
        if(withoutEscaped.contains("***"))
            throw new IllegalArgumentException(pattern+": Illegal sequence of wildcard characters");
        this.containsDoubleWildcard = withoutEscaped.contains("**");

        String regex = '^' + withoutEscaped
                .replace("/**", "(?:/." + (char) 0 + ")?")
                .replace("**/", ".+/")
                .replace("**", "." + (char) 0)
                .replace("*", "[^/]+")
                .replace((char) 0, '*') + '$';

        for(String innerRegex : innerRegexes) {
            int index = regex.indexOf(1);
            regex = regex.substring(0, index) + innerRegex + regex.substring(index + 1);
        }

        this.regex = Pattern.compile(regex);
    }

    public boolean matches(Route route) {
        return regex.matcher(route.toString()).matches();
    }

    public String getVariable(Route route, String varName) {
        if(!containsVariable(varName))
            throw new IllegalArgumentException("Route pattern does not include the variable");
        Matcher matcher = regex.matcher(route.toString());
        if(!matcher.matches())
            throw new IllegalArgumentException("Route does not match pattern");
        return URLDecoder.decode(matcher.group("v" + variables.indexOf(varName)), StandardCharsets.UTF_8);
    }

    public boolean containsVariable(String name) {
        return variables.contains(name);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RoutePattern && pattern.equals(((RoutePattern) obj).pattern);
    }

    @Override
    public int hashCode() {
        return pattern.hashCode();
    }

    @Override
    public String toString() {
        return pattern;
    }

    public static RoutePattern parse(String pattern) {
        return new RoutePattern(pattern);
    }

    public static boolean containsPattern(String pattern) {
        return pattern.replace("\\*", "").contains("*") || VAR_PATTERN.matcher(pattern).find();
    }

    public boolean containsDoubleWildcard() {
        return containsDoubleWildcard;
    }
}
