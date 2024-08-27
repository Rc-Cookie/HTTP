package de.rccookie.http;

import java.util.ArrayList;

import de.rccookie.util.Arguments;

final class EditableMultipart extends BufferableBody.Multipart implements Body.Multipart.Editable {

    EditableMultipart() {
        super(new ArrayList<>());
    }

    @Override
    public Editable setBoundary(String boundary) {
        this.boundary.value = Arguments.checkNull(boundary, "boundary");
        return this;
    }

    @Override
    public Editable add(Part part) {
        parts.add(Arguments.checkNull(part, "part"));
        return this;
    }
}
