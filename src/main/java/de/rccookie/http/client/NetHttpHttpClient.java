package de.rccookie.http.client;

import java.net.URL;

import de.rccookie.http.HttpRequest;
import de.rccookie.json.Json;
import de.rccookie.json.JsonObject;

final class NetHttpHttpClient implements HttpClient {

    static {
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "connection,content-length,expect,host,upgrade");
        Json.registerDeserializer(NetHttpHttpClient.class, json -> (NetHttpHttpClient) STD_NET_HTTP);
    }

    @Override
    public HttpRequest.Unsent get(URL url) {
        return new NetHttpHttpRequest(url);
    }

    @Override
    public Object toJson() {
        return new JsonObject("class", getClass());
    }
}
