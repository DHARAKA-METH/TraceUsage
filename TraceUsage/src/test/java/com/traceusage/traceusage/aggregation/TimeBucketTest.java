package com.traceusage.traceusage.aggregation;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TimeBucketTest {

    @Test
    void shouldTruncateInstantToUtcHour() {
        assertThat(TimeBucket.hour(Instant.parse("2026-10-14T10:37:25Z")))
                .isEqualTo(Instant.parse("2026-10-14T10:00:00Z"));
    }

    @Test
    void shouldKeepAlreadyTruncatedHour() {
        assertThat(TimeBucket.hour(Instant.parse("2026-10-14T10:00:00Z")))
                .isEqualTo(Instant.parse("2026-10-14T10:00:00Z"));
    }
}
