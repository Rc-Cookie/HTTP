package de.rccookie.http.server.processor;

import java.util.Locale;

import de.rccookie.http.HttpRequest;
import de.rccookie.http.server.HttpProcessor;
import de.rccookie.util.Arguments;

public class RuntimeStopwatchProcessor implements HttpProcessor {

    private final String headerField;

    public RuntimeStopwatchProcessor(String headerField) {
        this.headerField = Arguments.checkNull(headerField, "headerField");
    }

    public RuntimeStopwatchProcessor() {
        this("X-Runtime");
    }

    @Override
    public void preprocess(HttpRequest.Received request) {
        long start = System.currentTimeMillis();
        request.addResponseConfigurator(r ->
                r.setHeaderField(headerField, String.format(Locale.ROOT, "%.3f", (System.currentTimeMillis() - start) / 1000.0)));
    }
}
