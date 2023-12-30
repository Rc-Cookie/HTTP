package de.rccookie.http;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import de.rccookie.json.Json;
import de.rccookie.json.JsonSerializable;
import de.rccookie.util.Arguments;
import de.rccookie.util.ImmutableSet;
import de.rccookie.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

/**
 * Represents an immutable collection of content types. Additionally to regular set
 * capabilities, the {@link #contains(Object)} method will also return <code>true</code>
 * for content types which are indirectly contained in these content types, that is,
 * a contained mime pattern matches the given content type.
 */
public class ContentTypes implements ImmutableSet<ContentType>, JsonSerializable {

    private static final ContentTypes EMPTY = new ContentTypes(Set.of());
    static {
        Json.registerDeserializer(ContentTypes.class, json -> new ContentTypes(json.asSet(ContentType.class)));
    }

    /**
     * A content type collection which contains all content types.
     */
    public static final ContentTypes ANY = new ContentTypes(Set.of(ContentType.ANY));

    private final Set<ContentType> data;

    ContentTypes(Set<ContentType> data) {
        this.data = Arguments.checkNull(data, "data");
    }

    @Override
    public String toString() {
        return data.stream().map(Objects::toString).collect(Collectors.joining(","));
    }

    @Override
    public Object toJson() {
        return data;
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    /**
     * Returns whether the given content type is implicitly present in these content types,
     * that is, it is either explicitly present, or a contained content type is a mime type
     * pattern that the given type matches.
     *
     * @param o Element whose presence in this set is to be tested
     * @return Whether the object is a content type that is implicitly present in these content types
     */
    @Override
    public boolean contains(Object o) {
        return o instanceof ContentType && (
                data.contains(o) || data.stream().anyMatch(((ContentType)o)::matches));
    }

    @NotNull
    @Override
    public Iterator<ContentType> iterator() {
        return Utils.view(data.iterator());
    }

    @Override
    public Object @NotNull [] toArray() {
        return data.toArray();
    }

    @Override
    public <T> T @NotNull [] toArray(T @NotNull [] a) {
        return data.toArray(a);
    }

    /**
     * Returns whether all the given content types are implicitly present in these content types,
     * that is, it is either explicitly present, or a contained content type is a mime type
     * pattern that the given type matches.
     *
     * @param c Collection to be checked for containment in this set
     * @return Whether the objects are all content types that are implicitly present in these content types
     */
    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return c.stream().allMatch(this::contains);
    }

    /**
     * Returns whether all content types from these content types match the given content type category.
     *
     * @param category The mime type pattern to be tested
     * @return Whether <code>category.matches(t)</code> is true for all content types <code>t</code> in this set
     */
    public boolean allMatch(ContentType category) {
        return stream().allMatch(category::matches);
    }

    /**
     * Returns whether any content type from these content types matches the given content type category.
     *
     * @param category The mime type pattern to be tested
     * @return Whether <code>category.matches(t)</code> is true for at least one content types <code>t</code> in this set
     */
    public boolean anyMatches(ContentType category) {
        return stream().anyMatch(category::matches);
    }

    /**
     * Returns the content type pattern that best matches the given content type, that is, from
     * all explicitly contained content types that contain the given type it has the least number
     * of wildcards. If the type it explicitly contained in this set, the equal object from this
     * set will be returned (note that that might have different parameters). If the given type
     * is not implicitly within this set, <code>null</code> will be returned.
     *
     * @param type The type to get the best fitting explicitly contained type for
     * @return The most concrete explicitly contained type which is a supertype or the same as
     *         the given type, or <code>null</code>
     */
    public ContentType getBestFit(ContentType type) {
        ContentType best = null;
        for(ContentType c : Utils.iterate(stream().filter(type::matches)))
            if(best == null || best.contains(c))
                best = c;
        return best;
    }

    /**
     * Returns the weight of the given type based on the contained content types, or <code>0</code>
     * if it is not contained at all. If multiple type patterns in this set match the given type,
     * the weight from the subtype will be used.
     *
     * @param type The type to get the weight for
     * @return The weight of the type, or 0 if not present
     */
    @Range(from = 0, to = 1)
    public double getWeight(ContentType type) {
        ContentType best = getBestFit(type);
        return best != null ? best.weight() : 0;
    }

    /**
     * Returns the most preferred content type from the specified options, that is,
     * the content type which has the highest weight in these content types (see
     * {@link #getWeight(ContentType)} for more detail). If there are multiple equally
     * preferred options, the option first present in the collection is returned.
     *
     * @param options The non-empty collection of possible content types
     * @return The content type from the specified content types with the highest weight
     *         in these content types
     */
    @NotNull
    public ContentType getPreferred(Collection<? extends ContentType> options) {
        Arguments.checkNull(options, "options");

        ContentType preferred = null;
        double preferredWeight = -1;

        for(ContentType c : options) {
            double w = getWeight(c);
            if(w > preferredWeight) {
                preferred = c;
                preferredWeight = w;
            }
        }
        if(preferred == null)
            throw new IllegalArgumentException("At least one option required");
        return preferred;
    }

    /**
     * Returns the most preferred content type from the specified options, that is,
     * the content type which has the highest weight in these content types (see
     * {@link #getWeight(ContentType)} for more detail). If there are multiple equally
     * preferred options, the option first present in the array is returned.
     *
     * @param options The non-empty array of possible content types
     * @return The content type from the specified content types with the highest weight
     *         in these content types
     */
    public ContentType getPreferred(ContentType... options) {
        return getPreferred(Arrays.asList(Arguments.checkNull(options, "options")));
    }


    /**
     * Returns an empty content type set.
     *
     * @return An empty content type collection
     */
    public static ContentTypes of() {
        return EMPTY;
    }

    /**
     * Returns a content type set with the given elements.
     *
     * @param types The content type (patterns) to be contained in this set, may not contain duplicates
     * @return A content type set containing all given content types
     */
    public static ContentTypes of(ContentType... types) {
        Arguments.deepCheckNull(types, "types");
        return new ContentTypes(Set.of(types));
    }

    /**
     * Returns a content type set with the given elements.
     *
     * @param types The content type (patterns) to be contained in this set, may not contain duplicates
     * @return A content type set containing all given content types
     */
    public static ContentTypes of(Collection<? extends ContentType> types) {
        Arguments.checkNull(types, "types");
        return new ContentTypes(Set.copyOf(types));
    }
}
