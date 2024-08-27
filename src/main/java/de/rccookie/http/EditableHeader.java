package de.rccookie.http;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import de.rccookie.util.Arguments;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class EditableHeader implements Header {

    @Nullable
    private final HttpRequest context;
    private final Map<String, Values> data = new HashMap<>();

    EditableHeader(@Nullable HttpRequest context) {
        this.context = context;
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
    public Header.Values put(String key, Values value) {
        return data.put(Header.normalizeKey(Arguments.checkNull(key, "key")), EditableValues.of(value));
    }

    @Override
    public Values remove(Object key) {
        return data.remove(Header.normalizeKey(key));
    }

    @Override
    public void putAll(@NotNull Map<? extends String, ? extends Values> m) {
        m.forEach(this::put);
    }

    @Override
    public void clear() {
        data.clear();
    }

    @NotNull
    @Override
    public Set<String> keySet() {
        return data.keySet();
    }

    @NotNull
    @Override
    public Collection<Values> values() {
        return data.values();
    }

    @NotNull
    @Override
    public Set<Entry<String, Values>> entrySet() {
        return data.entrySet();
    }

    @Override
    public Values addSetCookie(@NotNull Cookie cookie) {
        return add("Set-Cookie", Arguments.checkNull(cookie, "cookie").toString(context != null ? context.route() : Route.ROOT));
    }


    static final class EditableValues implements Values {

        private final List<String> data = new ArrayList<>();

        EditableValues(List<? extends String> data) {
            addAll(data);
        }

        EditableValues(String... values) {
            this(values.length == 0 ? List.of() : List.of(values));
        }

        @Override
        public boolean equals(Object o) {
            return this == o || (o instanceof Header.Values && data.equals(o));
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
        public <T> T @NotNull [] toArray(@NotNull T @NotNull [] a) {
            return data.toArray(a);
        }

        @Override
        public boolean add(String s) {
            if(!ReadonlyHeader.ReadonlyValues.PATTERN.matcher(Arguments.checkNull(s, "s")).matches())
                throw new IllegalArgumentException("Illegal header value: " + s);
            return data.add(s);
        }

        @Override
        public boolean remove(Object o) {
            return data.remove(o);
        }

        @Override
        public boolean containsAll(@NotNull Collection<?> c) {
            //noinspection SlowListContainsAll
            return data.containsAll(c);
        }

        @Override
        public boolean addAll(@NotNull Collection<? extends String> c) {
            boolean diff = false;
            for(String s : c) diff |= add(s);
            return diff;
        }

        @Override
        public boolean addAll(int index, @NotNull Collection<? extends String> c) {
            for(String s : c) add(index++, s);
            return c.isEmpty();
        }

        @Override
        public boolean removeAll(@NotNull Collection<?> c) {
            return data.removeAll(c);
        }

        @Override
        public boolean retainAll(@NotNull Collection<?> c) {
            return data.retainAll(c);
        }

        @Override
        public void clear() {
            data.clear();
        }

        @Override
        public String get(int index) {
            return data.get(index);
        }

        @Override
        public String set(int index, String s) {
            if(!ReadonlyHeader.ReadonlyValues.PATTERN.matcher(Arguments.checkNull(s, "s")).matches())
                throw new IllegalArgumentException("Illegal header value: " + s);
            return data.set(index, s);
        }

        @Override
        public void add(int index, String s) {
            if(!ReadonlyHeader.ReadonlyValues.PATTERN.matcher(Arguments.checkNull(s, "s")).matches())
                throw new IllegalArgumentException("Illegal header value: " + s);
            data.add(index, s);
        }

        @Override
        public String remove(int index) {
            return data.remove(index);
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
                    it.remove();
                }

                @Override
                public void set(String s) {
                    if(!ReadonlyHeader.ReadonlyValues.PATTERN.matcher(Arguments.checkNull(s, "s")).matches())
                        throw new IllegalArgumentException("Illegal header value: " + s);
                    it.set(s);
                }

                @Override
                public void add(String s) {
                    if(!ReadonlyHeader.ReadonlyValues.PATTERN.matcher(Arguments.checkNull(s, "s")).matches())
                        throw new IllegalArgumentException("Illegal header value: " + s);
                    it.add(s);
                }
            };
        }

        @NotNull
        @Override
        public List<String> subList(int fromIndex, int toIndex) {
            throw new UnsupportedOperationException();
        }

        private static EditableValues of(Values values) {
            return values instanceof EditableValues ? (EditableValues) values : new EditableValues(values);
        }
    }
}
