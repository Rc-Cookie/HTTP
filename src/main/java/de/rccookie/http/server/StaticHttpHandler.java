package de.rccookie.http.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import de.rccookie.http.ContentType;
import de.rccookie.http.HttpRequest;
import de.rccookie.http.HttpResponse;
import de.rccookie.http.Path;
import de.rccookie.http.ResponseCode;
import de.rccookie.util.Arguments;
import de.rccookie.util.Utils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An http listener which serves from the bundled jar resources and/or from
 * the file system. For the jar resources, GET and HEAD are supported, for
 * the file system, the methods POST, PUT and DELETE are also supported
 * (where POST and PUT do exactly the same). The methods can be disabled
 * by simply not registering the handler for these methods. Note that empty
 * files will not be detected from the jar resources (they will from the
 * regular file system).
 * <p>GET and HEAD requests will guess the returned content type from the
 * file name. Additionally, if the file cannot be found, it will also be
 * tested for <code>[name].html</code> if the name has no extension, or, if
 * the file is a directory, for <code>index.html</code> and <code>main.html</code>
 * within the directory.</p>
 */
public class StaticHttpHandler implements HttpRequestHandler {

    @Nullable
    private final java.nio.file.Path fileRoot;
    @Nullable
    private final Path resourceRoot;
    private final Mapper mapper;

    private boolean allowOverride = false;
    private DirectoryDeleteMode directoryDeleteMode = DirectoryDeleteMode.NEVER;

    private final Map<Path, Boolean> resourceDirectoryCache = new HashMap<>();

    @Contract("null,null,_->fail")
    public StaticHttpHandler(@Nullable java.nio.file.Path fileRoot, @Nullable Path resourceRoot, @Nullable Mapper mapper) {
        if(fileRoot == null)
            this.fileRoot = null;
        else {
            this.fileRoot = fileRoot.toAbsolutePath().normalize();
            if(!Files.exists(this.fileRoot))
                throw new IllegalArgumentException("File root does not exist");
        }
        this.resourceRoot = resourceRoot;
        if(fileRoot == null && resourceRoot == null)
            throw new IllegalArgumentException("At least one of fileRoot and resourceRoot must be present");
        this.mapper = mapper != null ? mapper : HttpRequest::path;
    }

    public StaticHttpHandler(@NotNull java.nio.file.Path fileRoot, @Nullable Mapper mapper) {
        this(fileRoot, null, mapper);
    }

    public StaticHttpHandler(@NotNull Path resourceRoot, @Nullable Mapper mapper) {
        this(null, resourceRoot, mapper);
    }

    public StaticHttpHandler(@NotNull java.nio.file.Path fileRoot) {
        this(fileRoot, null);
    }

    public StaticHttpHandler(@NotNull Path resourceRoot) {
        this(resourceRoot, null);
    }


    /**
     * Returns whether PUT requests are allowed to replace existing files. <code>false</code>
     * by default. This is irrelevant if the handler is not registered for PUT requests.
     *
     * @return whether PUT requests can override existing files
     */
    public boolean isAllowOverride() {
        return allowOverride;
    }

    /**
     * Sets whether PUT requests are allowed to replace existing files. <code>false</code>
     * by default. This is irrelevant if the handler is not registered for PUT requests.
     *
     * @param allowOverride Whether PUT requests can override existing files
     */
    public void setAllowOverride(boolean allowOverride) {
        this.allowOverride = allowOverride;
    }

    /**
     * Returns the directory delete mode for DELETE requests.
     *
     * @return The current directory delete mode
     */
    public DirectoryDeleteMode getDirectoryDeleteMode() {
        return directoryDeleteMode;
    }

    /**
     * Sets the directory delete mode for DELETE requests
     *
     * @param directoryDeleteMode The directory delete mode to use
     */
    public void setDirectoryDeleteMode(DirectoryDeleteMode directoryDeleteMode) {
        this.directoryDeleteMode = Arguments.checkNull(directoryDeleteMode, "directoryDeleteMode");
    }


    @Override
    public void respond(HttpRequest.Received request) throws Exception {
        String path = mapper.remap(request).toString().substring(1);

        if(request.method() == HttpRequest.Method.GET || request.method() == HttpRequest.Method.HEAD) {
            if(getFromFS(request, path)) return;
            if(getFromResources(request, path)) return;
        }
        if(request.method() == HttpRequest.Method.PUT || request.method() == HttpRequest.Method.POST) {
            putToFS(request, path);
            return;
        }
        if(request.method() == HttpRequest.Method.DELETE) {
            deleteFromFS(request, path);
            return;
        }

        throw HttpRequestFailure.notFound();
    }

    private boolean getFromFS(HttpRequest.Received request, String path) throws Exception {
        if(fileRoot == null) return false;
        java.nio.file.Path p = fileRoot.resolve(path).toAbsolutePath().normalize();
        if(!p.startsWith(fileRoot)) return false;

        if(!Files.exists(p)) {
            // Test whether name.html exists
            String name = p.getFileName().toString();
            if(name.contains(".")) return false; // Has file suffix
            p = p.resolveSibling(name+".html");

        } else if(Files.isDirectory(p)) {
            // Test whether name/index.html or name/main.html exists
            p = p.resolve("index.html");
            if(!Files.exists(p) || Files.isDirectory(p))
                p = p.resolveSibling("main.html");
        }
        if(!Files.exists(p) || Files.isDirectory(p)) return false;

        request.respond(ResponseCode.OK)
                .setContentType(ContentType.guessFromName(p.getFileName().toString()))
                .setStream(Files.newInputStream(p));
        return true;
    }

    private boolean getFromResources(HttpRequest.Received request, String path) throws Exception {
        if(resourceRoot == null) return false;
        Path p = resourceRoot.resolve(path).normalize();
        if(!p.startsWith(resourceRoot)) return false;

        URL resource = getClass().getResource(p.toString());
        boolean dir = isDirectory(p, resource);
        if(resource == null || dir) {
            // Test whether name.html exists
            String name = p.getFileName();
            if(name.contains(".")) return false; // Has file suffix

            p = p.resolveSibling(name+".html");
            resource = getClass().getResource(p.toString());
        }
        else if(dir) {
            // Test whether name/index.html or name/main.html exists
            p = p.resolve("index.html");
            resource = getClass().getResource(p.toString());
            if(resource == null) {
                p = p.resolveSibling("main.html");
                resource = getClass().getResource(p.toString());
            }
        }
        if(resource == null) return false;

        HttpResponse.Sendable resp = request.respond(ResponseCode.OK)
                .setContentType(ContentType.guessFromName(p.getFileName()));
        if(request.method() != HttpRequest.Method.HEAD)
            resp.setStream(resource.openStream());
        return true;
    }

    private boolean isDirectory(Path path, URL resource) {
        if(resource == null) return false;
        synchronized(resourceDirectoryCache) {
            return resourceDirectoryCache.computeIfAbsent(path, this::testIsDirectory);
        }
    }

    private boolean testIsDirectory(Path path) {
        URL resource = getClass().getResource(path.toString());
        if(resource == null) return false;
        try(BufferedReader in = new BufferedReader(new InputStreamReader(resource.openStream()))) {
            Set<String> found = new HashSet<>();
            // This is also true for empty files :/
            return in.lines().allMatch(l -> found.add(l) && getClass().getResource(path.resolve(l).toString()) != null);
        } catch(InvalidPathException e) {
            return false;
        } catch(IOException e) {
            throw Utils.rethrow(e);
        }
    }

    private void putToFS(HttpRequest.Received request, String path) throws Exception {
        if(fileRoot == null)
            throw HttpRequestFailure.methodNotAllowed(HttpRequest.Method.PUT, null);
        java.nio.file.Path p = fileRoot.resolve(path).toAbsolutePath().normalize();
        if(!p.startsWith(fileRoot))
            throw new HttpRequestFailure(ResponseCode.FORBIDDEN, "Resource cannot be accessed");

        if(Files.exists(p) && (!isAllowOverride() || !Files.isRegularFile(p)))
            throw new HttpRequestFailure(ResponseCode.CONFLICT, "Cannot write resource");
        else Files.createDirectories(p.getParent());

        try (InputStream in = request.body().stream();
             OutputStream out = Files.newOutputStream(p)) {
            in.transferTo(out);
        }

        request.respond(ResponseCode.NO_CONTENT);
    }

    private void deleteFromFS(HttpRequest.Received request, String path) throws Exception {
        if(fileRoot == null)
            throw HttpRequestFailure.methodNotAllowed(HttpRequest.Method.DELETE, null);
        java.nio.file.Path p = fileRoot.resolve(path).toAbsolutePath().normalize();
        if(!p.startsWith(fileRoot) || (Files.exists(p) && Files.isSameFile(fileRoot, p)))
            throw HttpRequestFailure.notFound();

        if(!Files.exists(p))
            throw HttpRequestFailure.notFound();
        if(Files.isDirectory(p)) {
            if(directoryDeleteMode == DirectoryDeleteMode.NEVER)
                throw new HttpRequestFailure(ResponseCode.FORBIDDEN, "Cannot delete directory");
            if(directoryDeleteMode == DirectoryDeleteMode.WITH_URL_PARAM &&
                    !request.query().containsKey("recursive"))
                throw new HttpRequestFailure(ResponseCode.UNAUTHORIZED, "Cannot delete directory without 'recursive' url parameter switch");

            //noinspection resource
            Files.walk(p)
                    .sorted(Comparator.reverseOrder())
                    .map(java.nio.file.Path::toFile)
                    .forEach(File::delete);
        }
        else if(!Files.isRegularFile(p))
            throw new HttpRequestFailure(ResponseCode.FORBIDDEN, "Resource cannot be accessed");
        else if(directoryDeleteMode == DirectoryDeleteMode.WITH_URL_PARAM &&
                request.query().containsKey("recursive"))
            throw new HttpRequestFailure(ResponseCode.BAD_REQUEST, "Cannot delete regular file with 'recursive' url parameter switch");
        else Files.delete(p);

        // Cleanup directories
        p = p.getParent().normalize();
        while(!Files.isSameFile(fileRoot, p) && p.startsWith(fileRoot) /* just to be sure */) {
            try(Stream<java.nio.file.Path> list = Files.list(p)) {
                if(list.findAny().isPresent()) break;
            }
            Files.delete(p);
            p = p.getParent().normalize();
        }

        request.respond(ResponseCode.NO_CONTENT);
    }


    /**
     * A mapping function to map received paths to the path relative to the specified root
     * directory from which to read the resource.
     */
    @FunctionalInterface
    public interface Mapper {
        /**
         * Simply returns the raw path from the request.
         */
        Mapper IDENTITY = HttpRequest::path;

        /**
         * Returns the path to the resource to be returned for the given request (may or may
         * not exist), relative to the specified root directory/directories.
         *
         * @param request The request to get the path for
         * @return The relative path the requested resource
         */
        Path remap(HttpRequest request);
    }

    /**
     * Defines what access the DELETE request has when attempting to delete a directory. The
     * root directory can never be deleted. Empty directories are deleted automatically when
     * the last file gets deleted, thus there is no purpose for an option ONLY_EMPTY.
     */
    public enum DirectoryDeleteMode {
        /**
         * DELETE requests cannot delete non-empty directories.
         */
        NEVER,
        /**
         * DELETE requests can delete non-empty directories, but only when an url parameter
         * <code>"recursive"</code> is present in the request query. Additionally, said flag
         * must not be present when deleting regular files.
         */
        WITH_URL_PARAM,
        /**
         * DELETE requests can recursively delete any directory just like any other resource.
         */
        ALWAYS
    }
}
