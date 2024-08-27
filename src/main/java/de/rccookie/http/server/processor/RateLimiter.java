package de.rccookie.http.server.processor;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import de.rccookie.http.HttpRequest;
import de.rccookie.http.header.RateLimit;
import de.rccookie.http.server.HttpProcessor;
import de.rccookie.http.server.HttpRequestFailure;
import de.rccookie.http.server.ThrowingRunnable;
import de.rccookie.util.Arguments;

public class RateLimiter implements HttpProcessor {

    private final String headerPrefix;
    private boolean includeDetail = true;
    private final int limit;
    private final long interval;

    private final Map<InetAddress, Rate> rates = new ConcurrentHashMap<>();

    public RateLimiter(String headerPrefix, int limit, long interval, TimeUnit timeUnit) {
        this.headerPrefix = Arguments.checkNull(headerPrefix, "headerPrefix");
        this.limit = Arguments.checkRange(limit, 1, null);
        this.interval = Arguments.checkNull(timeUnit, "timeUnit").toSeconds(Arguments.checkRange(interval, 1L, null));
    }

    public RateLimiter(RateLimit.Naming naming, int limit, long interval, TimeUnit timeUnit) {
        this(Arguments.checkNull(naming, "naming").headerPrefix(), limit, interval, timeUnit);
    }

    public boolean includeDetail() {
        return includeDetail;
    }

    public void setIncludeDetail(boolean includeDetail) {
        this.includeDetail = includeDetail;
    }

    @Override
    public void process(HttpRequest.Received request, ThrowingRunnable runHandler) throws Exception {
        InetAddress client = request.client().getAddress();
        Rate rate = rates.computeIfAbsent(client, $ -> new Rate());
        RateLimit rateLimit;
        synchronized(rate) {
            long time = System.currentTimeMillis() / 1000;
            if(rate.timestamp + interval <= time) {
                rate.timestamp = time;
                rate.count = 1;
            }
            else if(rate.count < limit)
                rate.count++;
            else if(includeDetail)
                throw HttpRequestFailure.tooManyRequests(new RateLimit(limit, 0, Instant.ofEpochSecond(rate.timestamp)), headerPrefix);
            else throw HttpRequestFailure.tooManyRequests();

            rateLimit = new RateLimit(limit, limit - rate.count, Instant.ofEpochSecond(rate.timestamp));
        }
        if(includeDetail)
            request.addResponseConfigurator(r -> r.header().setRateLimit(rateLimit, headerPrefix));
        runHandler.run();
    }

    private static final class Rate {
        public long timestamp;
        public int count = 0;
    }
}
