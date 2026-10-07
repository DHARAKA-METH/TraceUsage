package com.traceusage.sdk.telemetry;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class TelemetryQueue {

    private final BlockingQueue<UsageEvent> queue;

    public TelemetryQueue(int capacity) {
        this.queue = new ArrayBlockingQueue<>(Math.max(1, capacity));
    }

    public boolean publish(UsageEvent event) {
        return queue.offer(event);
    }

    public UsageEvent poll(long timeout, TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }

    public int drainTo(Collection<UsageEvent> events, int maxElements) {
        return queue.drainTo(events, maxElements);
    }
}
