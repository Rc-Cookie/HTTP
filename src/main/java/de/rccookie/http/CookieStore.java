package de.rccookie.http;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import de.rccookie.util.Arguments;
import de.rccookie.util.IterableIterator;
import de.rccookie.util.ListStream;
import org.jetbrains.annotations.NotNull;

public class CookieStore implements Iterable<Cookie> {

    private final Map<String, Cookie> cookies = new HashMap<>();
    private Set<Cookie> cookiesSet = null;

    @Override
    public String toString() {
        return Cookie.toString(cookies.values());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CookieStore && cookies.equals(((CookieStore) obj).cookies);
    }

    @Override
    public int hashCode() {
        return cookies.hashCode();
    }

    public int size() {
        return cookies.size();
    }

    public boolean isEmpty() {
        return cookies.isEmpty();
    }

    public boolean contains(String cookie) {
        return cookies.containsKey(cookie);
    }

    public boolean contains(Cookie cookie) {
        return Objects.equals(cookies.get(Arguments.checkNull(cookie, "cookie").name()), cookie);
    }

    @Override
    @NotNull
    public IterableIterator<Cookie> iterator() {
        return IterableIterator.iterator(cookies.values());
    }

    public ListStream<Cookie> stream() {
        return ListStream.of(cookies.values());
    }

    @NotNull
    public Cookie[] toArray() {
        return cookies.values().toArray(Cookie[]::new);
    }

    public Cookie get(String name) {
        return cookies.get(name);
    }

    public Cookie put(Cookie cookie) {
        return cookies.put(Arguments.checkNull(cookie, "cookie").name(), cookie);
    }

    public boolean remove(String cookie) {
        return cookies.remove(Arguments.checkNull(cookie, "cookie")) != null;
    }

    public boolean remove(Cookie cookie) {
        return cookies.remove(Arguments.checkNull(cookie, "cookie").name(), cookie);
    }

    public boolean containsAll(@NotNull Collection<? extends String> c) {
        return cookies.keySet().containsAll(c);
    }

    public void putAll(@NotNull Collection<? extends Cookie> c) {
        c.forEach(this::put);
    }

    public void putAll(HttpResponse r) {
        putAll(r.cookies().values());
    }

    public boolean retainAll(@NotNull Collection<? extends String> c) {
        return cookies.keySet().retainAll(c);
    }

    public boolean removeIf(Predicate<? super Cookie> filter) {
        return cookies.values().removeIf(filter);
    }

    public boolean removeAll(@NotNull Collection<? extends String> c) {
        boolean any = false;
        for(String cookie : c)
            any |= remove(cookie);
        return any;
    }

    public void clear() {
        cookies.clear();
    }

    public Set<String> names() {
        return cookies.keySet();
    }

    public Set<Cookie> cookies() {
        if(cookiesSet != null) return cookiesSet;
        return cookiesSet = new Set<>() {
            @Override
            public int size() {
                return cookies.size();
            }

            @Override
            public boolean isEmpty() {
                return cookies.isEmpty();
            }

            @Override
            public boolean contains(Object o) {
                //noinspection SuspiciousMethodCalls
                return cookies.containsValue(o);
            }

            @NotNull
            @Override
            public Iterator<Cookie> iterator() {
                return CookieStore.this.iterator();
            }

            @NotNull
            @Override
            public Object @NotNull [] toArray() {
                return cookies.values().toArray();
            }

            @NotNull
            @Override
            public <T> T @NotNull [] toArray(@NotNull T @NotNull [] a) {
                return cookies.values().toArray(a);
            }

            @Override
            public boolean add(Cookie cookie) {
                return !Objects.equals(cookies.put(cookie.name(), cookie), cookie);
            }

            @Override
            public boolean remove(Object o) {
                return o instanceof Cookie && CookieStore.this.remove((Cookie) o);
            }

            @Override
            public boolean containsAll(@NotNull Collection<?> c) {
                return cookies.values().containsAll(c);
            }

            @Override
            public boolean addAll(@NotNull Collection<? extends Cookie> c) {
                boolean mod = false;
                for(Cookie cookie : c)
                    mod |= add(cookie);
                return mod;
            }

            @Override
            public boolean retainAll(@NotNull Collection<?> c) {
                return cookies.values().retainAll(c);
            }

            @Override
            public boolean removeAll(@NotNull Collection<?> c) {
                return cookies.values().removeAll(c);
            }

            @Override
            public void clear() {
                cookies.clear();
            }
        };
    }
}
