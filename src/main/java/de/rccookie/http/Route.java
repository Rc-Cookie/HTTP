package de.rccookie.http;

import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import de.rccookie.json.Json;
import de.rccookie.json.JsonSerializable;
import de.rccookie.util.Arguments;
import de.rccookie.util.Utils;
import org.jetbrains.annotations.NotNull;

public final class Route implements CharSequence, Iterable<String>, JsonSerializable {

    static {
        Json.registerDeserializer(Route.class, json -> json.isString() ? of(json.asString()) : ofNames(json.asArray(String.class)));
    }

    public static final Route ROOT = new Route(List.of());

    private final List<String> names;
    private String asString;

    private Route(List<String> names) {
        this.names = Arguments.checkNull(names, "names");
    }

    @Override
    public int length() {
        return toString().length();
    }

    @Override
    public char charAt(int index) {
        return toString().charAt(index);
    }

    @NotNull
    @Override
    public String subSequence(int start, int end) {
        return toString().substring(start, end);
    }

    @Override
    public @NotNull String toString() {
        if(asString == null)
            asString = "/" + names.stream().map(n -> URLEncoder.encode(n, StandardCharsets.UTF_8)).collect(Collectors.joining("/"));
        return asString;
    }

    @Override
    public Object toJson() {
        return toString();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Route && names.equals(((Route) obj).names);
    }

    public boolean equalsStr(String path) {
        try {
            return path.startsWith("/") && equals(of(path));
        } catch(Exception e) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return names.hashCode();
    }

    public Path toFilePath() {
        return Path.of(toString().substring(1));
    }

    public boolean startsWith(String start) {
        if(start.isEmpty())
            return true;
        if(!start.startsWith("/"))
            return false;
        try {
            return toString().startsWith(of(start).toString());
        } catch(Exception e) {
            return false;
        }
    }

    public boolean startsWith(Route route) {
        if(route.names.size() > names.size())
            return false;
        return names.subList(0, route.names.size()).equals(route.names);
    }

    public boolean isRoot() {
        return names.isEmpty();
    }

    public Route getParent() {
        if(names.isEmpty())
            return null;
        return new Route(names.subList(0, names.size() - 1));
    }

    public Route normalize() {
        ArrayList<String> normalized = new ArrayList<>();
        for(String name : names) {
            if(name.isEmpty() || name.equals("."))
                continue;
            if(name.equals("..") && !normalized.isEmpty())
                normalized.remove(normalized.size() - 1);
            else normalized.add(name);
        }
        normalized.trimToSize();
        return new Route(normalized);
    }

    public Route resolve(String path) {
        return resolve(of(path.startsWith("/") ? path : "/" + path));
    }

    public Route resolveNames(String... names) {
        return resolve(Route.ofNames(names));
    }

    public Route resolve(Route path) {
        List<String> resolved = new ArrayList<>(names.size() + path.names.size());
        resolved.addAll(names);
        resolved.addAll(path.names);
        return new Route(resolved);
    }

    public Route resolveSibling(String path) {
        return resolveSibling(of(path.startsWith("/") ? path : "/" + path));
    }

    public Route resolveSiblingNames(String... names) {
        return resolveSibling(Route.ofNames(names));
    }

    public Route resolveSibling(Route path) {
        Route parent = getParent();
        if(parent == null)
            return path;
        return parent.resolve(path);
    }

    public int getNameCount() {
        return names.size();
    }

    public String getName(int index) {
        return names.get(index);
    }

    public String getFileName() {
        return names.isEmpty() ? "" : names.get(names.size() - 1);
    }

    public Route subRoute(int start, int end) {
        if(start == 0 && end == names.size())
            return this;
        return new Route(names.subList(start, end));
    }

    public Route subRoute(int start) {
        return subRoute(start, names.size());
    }


    @NotNull
    @Override
    public Iterator<String> iterator() {
        return Utils.view(names.iterator());
    }



    public static Route fromUrl(URL url) {
        String path = url.getPath();
        return of(path.startsWith("/") ? path : "/" + path);
    }

    public static Route of(String route) {
        if(!Arguments.checkNull(route, "route").startsWith("/"))
            throw new IllegalArgumentException("Route must start with '/'");

        ArrayList<String> parts = new ArrayList<>();
        int start = 1, end;
        while(start < route.length() && (end = route.indexOf('/', start)) >= 0) {
            parts.add(URLDecoder.decode(route.substring(start, end), StandardCharsets.UTF_8));
            start = end + 1;
        }
        if(start < route.length())
            parts.add(URLDecoder.decode(route.substring(start), StandardCharsets.UTF_8));

        parts.trimToSize();
        return new Route(parts);
    }

    public static Route ofNames(String... names) {
        return new Route(List.of(Arguments.deepCheckNull(names, "names")));
    }
}
