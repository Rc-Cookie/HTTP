package de.rccookie.http.client;

import de.rccookie.util.Arguments;

final class HttpClientContainer {

    private HttpClientContainer() { }

    private static HttpClient currentDefault = HttpClient.STD_NET;

    public static HttpClient getDefault() {
        return currentDefault;
    }

    public static void setDefault(HttpClient client) {
        currentDefault = Arguments.checkNull(client, "client");
    }
}
