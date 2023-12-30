package de.rccookie.http;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Objects;

import de.rccookie.json.Json;
import de.rccookie.json.JsonSerializable;
import de.rccookie.util.Arguments;
import de.rccookie.util.MappingIterator;
import org.jetbrains.annotations.NotNull;

public final class Path implements CharSequence, Iterable<String>, JsonSerializable {

    static {
        Json.registerDeserializer(Path.class, json -> new Path(json.as(java.nio.file.Path.class)));
    }

    public static final Path ROOT = new Path(java.nio.file.Path.of("/"));

    private final java.nio.file.Path path;

    private Path(java.nio.file.Path path) {
        this.path = Arguments.checkNull(path, "path");
    }

    @Override
    public int length() {
        return path.toString().length();
    }

    @Override
    public char charAt(int index) {
        char c = path.toString().charAt(index);
        return c == File.separatorChar ? '/' : c;
    }

    @NotNull
    @Override
    public String subSequence(int start, int end) {
        return toString().substring(start, end);
    }

    @Override
    public @NotNull String toString() {
        return path.toString().replace(File.separatorChar, '/');
    }

    @Override
    public Object toJson() {
        return path;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Path && path.equals(((Path) obj).path);
    }

    public boolean equalsStr(String path) {
        return toString().equals(path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    public java.nio.file.Path toFilePath() {
        return java.nio.file.Path.of(path.toString().substring(1));
    }

    public boolean startsWith(String start) {
        return toString().startsWith(start);
    }

    public boolean startsWith(Path path) {
        return this.path.startsWith(path.path);
    }

    public boolean isRoot() {
        return path.getNameCount() == 0;
    }

    public Path getParent() {
        java.nio.file.Path parent = path.getParent();
        return parent != null ? new Path(parent) : null;
    }

    public Path normalize() {
        return new Path(path.normalize());
    }

    public Path resolve(String path) {
        return new Path(this.path.resolve(path));
    }

    public Path resolveSibling(String path) {
        Path parent = getParent();
        if(parent == null) {
            if(path.startsWith("/"))
                return resolve(path.substring(1));
            throw new IllegalStateException("Cannot resolve sibling of root");
        }
        return parent.resolve(path);
    }

    public int getNameCount(int count) {
        return path.getNameCount();
    }

    public String getName(int index) {
        return path.getName(index).toString();
    }

    public String getFileName() {
        String name = Objects.toString(path.getFileName());
        return name != null ? name : "";
    }

    @NotNull
    @Override
    public Iterator<String> iterator() {
        return new MappingIterator<>(path, Object::toString);
    }



    public static Path fromUrl(URL url) {
        String path = url.getPath();
        return path.isBlank() ? ROOT : new Path(java.nio.file.Path.of(url.getPath()));
    }

    public static Path of(String path) {
        if(!Arguments.checkNull(path, "path").startsWith("/"))
            throw new IllegalArgumentException("Path must start with '/'");
        try {
            return fromUrl(new URL("https://"+path));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Illegal path:", e);
        }
    }
}
