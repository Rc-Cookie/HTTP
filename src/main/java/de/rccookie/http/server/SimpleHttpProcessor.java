package de.rccookie.http.server;

import java.lang.annotation.Annotation;

import de.rccookie.util.Arguments;

public class SimpleHttpProcessor<A extends Annotation> implements HttpProcessor {

    protected final A config;

    public SimpleHttpProcessor(A config) {
        this.config = Arguments.checkNull(config, "config");
    }
}
