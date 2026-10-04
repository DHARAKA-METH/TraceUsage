package com.demo.traceusage;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
public class TelemetryQueue {

    private final BlockingQueue<UsageEvent> queue = new ArrayBlockingQueue<>(10_000);

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
