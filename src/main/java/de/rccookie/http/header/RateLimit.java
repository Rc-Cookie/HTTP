package de.rccookie.http.header;

import java.time.Duration;
import java.time.Instant;

import de.rccookie.util.Arguments;

public final class RateLimit {

    private final int limit;
    private final int remaining;
    private final Instant reset;

    public RateLimit(int limit, int remaining, Instant reset) {
        this.limit = Arguments.checkRange(limit, 0, null);
        this.remaining = Arguments.checkRange(remaining, 0, limit+1);
        this.reset = Arguments.checkNull(reset, "reset");
    }

    public int limit() {
        return limit;
    }

    public int remaining() {
        return remaining;
    }

    public int used() {
        return limit - remaining;
    }

    public Instant reset() {
        return reset;
    }

    public Duration timeUntilReset() {
        return Duration.between(Instant.now(), reset);
    }


    public enum Naming {
        X_RateLimit,
        X_Rate_Limit;

        public String headerPrefix() {
            return name().replace('_', '-');
        }
    }
}
