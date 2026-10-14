package com.traceusage.traceusage.aggregation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class TimeBucket {

    private TimeBucket() {
    }

    public static Instant hour(Instant timestamp) {
        return Objects.requireNonNull(timestamp, "timestamp must not be null")
                .truncatedTo(ChronoUnit.HOURS);
    }
}
