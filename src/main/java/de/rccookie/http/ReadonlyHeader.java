package de.rccookie.http;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import de.rccookie.http.auth.AuthChallenge;
import de.rccookie.http.useragent.UserAgent;
import de.rccookie.util.Console;
import de.rccookie.util.Utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ReadonlyHeader implements Header {

    private final Map<String, Values> data;
    private Set<String> keySet = null;
    private Collection<Values> values = null;
    private Set<Entry<String, Values>> entrySet = null;

    private Instant date = null;
    private ContentTypes accept = null;
    private Map<String, Cookie> cookies = null;
    private boolean cookiesIsSetCookies = false;
    private UserAgent userAgent = null;
    private List<AuthChallenge> wwwAuthenticate = null;
    private List<AuthChallenge> proxyAuthenticate = null;

    ReadonlyHeader(@NotNull Map<? extends String, ? extends List<? extends String>> map) {
        data = new HashMap<>(map.size());
        map.forEach((k,v) -> { if(k != null) data.put(Header.normalizeKey(k), new ReadonlyValues(v)); });
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Header && data.equals(o));
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        data.forEach((k,vs) -> vs.forEach(v -> str.append(k).append(": ").append(v).append("\r\n")));
        return str.toString();
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return data.containsKey(Header.normalizeKey(key));
    }

    @Override
    public boolean containsValue(Object value) {
        return data.containsValue(value);
    }

    @Override
    public Values get(Object key) {
        return data.get(Header.normalizeKey(key));
    }

    @Nullable
    @Override
    public Header.Values put(String key, Values values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Values remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(@NotNull Map<? extends String, ? extends Values> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Set<String> keySet() {
        return keySet != null ? keySet : (keySet = Utils.view(data.keySet()));
    }

    @NotNull
    @Override
    public Collection<Values> values() {
        return values != null ? values : (values = Utils.view(data.values()));
    }

    @NotNull
    @Override
    public Set<Entry<String, Values>> entrySet() {
        return entrySet != null ? entrySet : (entrySet = data.entrySet());
    }


    @Override
    public Values addSetCookie(@NotNull Cookie cookie) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Instant getDate() {
        if(date == null)
            date = Header.super.getDate();
        return date;
    }

    @Override
    public @NotNull Map<String, Cookie> getCookies() {
        if(cookies == null || cookiesIsSetCookies) {
            try {
                cookies = Header.super.getCookies();
            } catch(Exception e) {
                Console.warn("Failed to parse received cookies:");
                Console.warn(e);
                cookies = Map.of();
            }
            cookiesIsSetCookies = false;
        }
        return cookies;
    }

    @Override
    public @NotNull Map<String, Cookie> getSetCookies() {
        if(cookies == null || !cookiesIsSetCookies) {
            cookies = Header.super.getSetCookies();
            cookiesIsSetCookies = true;
        }
        return cookies;
    }

    @Override
    public @NotNull ContentTypes getAccept() {
        if(accept == null) try {
            accept = Header.super.getAccept();
        } catch(Exception e) {
            Console.warn("Failed to parse received accept header:");
            Console.warn(e);
            accept = ContentTypes.ANY;
        }
        return accept;
    }

    @Override
    public ContentType getContentType() {
        try {
            return Header.super.getContentType();
        } catch(Exception e) {
            Console.warn("Failed to parse received content type header:");
            Console.warn(e);
            return null;
        }
    }

    @Override
    public @NotNull UserAgent getUserAgent() {
        // No parsing here, but UserAgent itself caches its results, so we should return the same instance
        if(userAgent == null)
            userAgent = Header.super.getUserAgent();
        return userAgent;
    }

    @Override
    public @NotNull List<AuthChallenge> getAuthenticate() {
        if(wwwAuthenticate == null)
            wwwAuthenticate = Utils.view(Header.super.getAuthenticate());
        return wwwAuthenticate;
    }

    @Override
    public @NotNull List<AuthChallenge> getProxyAuthenticate() {
        if(proxyAuthenticate == null)
            proxyAuthenticate = Utils.view(Header.super.getProxyAuthenticate());
        return proxyAuthenticate;
    }

    static final class ReadonlyValues implements Values {

        static final Pattern PATTERN = Pattern.compile("[a-zA-Z0-9_ :;.,/\\\\\"'?!(){}\\[\\]@<>=\\-+*#$&`|~^%]*");

        private final List<String> data;

        ReadonlyValues(List<? extends String> data) {
            this.data = new ArrayList<>(data);
            for(String v : data)
                if(!PATTERN.matcher(v).matches())
                    throw new IllegalArgumentException("Illegal header value: " + v);
        }

        ReadonlyValues(String... values) {
            this(values.length == 0 ? List.of() : List.of(values));
        }

        @Override
        public boolean equals(Object o) {
            return this == o || (o instanceof ReadonlyValues && data.equals(o));
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return String.join(", ", data);
        }

        @Override
        public int size() {
            return data.size();
        }

        @Override
        public boolean isEmpty() {
            return data.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return data.contains(o);
        }

        @NotNull
        @Override
        public Iterator<String> iterator() {
            return listIterator();
        }

        @NotNull
        @Override
        public Object @NotNull [] toArray() {
            return data.toArray();
        }

        @Override
        public <T> T @NotNull [] toArray(T @NotNull [] a) {
            return data.toArray(a);
        }

        @Override
        public boolean add(String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(@NotNull Collection<?> c) {
            //noinspection SlowListContainsAll
            return data.containsAll(c);
        }

        @Override
        public boolean addAll(@NotNull Collection<? extends String> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(int index, @NotNull Collection<? extends String> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(@NotNull Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(@NotNull Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String get(int index) {
            return data.get(index);
        }

        @Override
        public String set(int index, String element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(int index, String element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String remove(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int indexOf(Object o) {
            return data.indexOf(o);
        }

        @Override
        public int lastIndexOf(Object o) {
            return data.lastIndexOf(o);
        }

        @NotNull
        @Override
        public ListIterator<String> listIterator() {
            return listIterator(0);
        }

        @NotNull
        @Override
        public ListIterator<String> listIterator(int index) {
            ListIterator<String> it = data.listIterator(index);
            return new ListIterator<>() {
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public String next() {
                    return it.next();
                }

                @Override
                public boolean hasPrevious() {
                    return it.hasPrevious();
                }

                @Override
                public String previous() {
                    return it.previous();
                }

                @Override
                public int nextIndex() {
                    return it.nextIndex();
                }

                @Override
                public int previousIndex() {
                    return it.previousIndex();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void set(String s) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void add(String s) {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @NotNull
        @Override
        public List<String> subList(int fromIndex, int toIndex) {
            return Utils.view(data.subList(fromIndex, toIndex));
        }
    }
}
