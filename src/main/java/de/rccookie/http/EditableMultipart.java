package de.rccookie.http;

import java.util.LinkedHashMap;
import java.util.Map;

import de.rccookie.util.Arguments;
import de.rccookie.util.Utils;

final class EditableMultipart implements Body.Multipart.Editable {

    private final String boundary = Long.toHexString(System.currentTimeMillis()) + Long.toHexString(System.nanoTime());

    private final Map<String, Part> parts = new LinkedHashMap<>();

    @Override
    public String boundary() {
        return boundary;
    }

    @Override
    public Map<String,Part> parts() {
        return Utils.view(parts);
    }

    @Override
    public Editable add(Part part) {
        parts.put(Arguments.checkNull(part, "part").name(), part);
        return this;
    }
}
