package org.nocturne.prometheus;

import io.prometheus.client.Counter;
import io.prometheus.client.Summary;

public class Prometheus {
    private static final Counter PAGES_COUNTER = Counter.build()
            .name("nocturne_pages_total").help("Nocturne pages total count")
            .labelNames("className")
            .register();

    private static final Summary PAGES_LATENCY_SECONDS = Summary.build()
            .name("nocturne_pages_latency_seconds").help("Nocturne pages latency in seconds")
            .labelNames("className", "phase")
            .register();

    private static final Counter FRAMES_COUNTER = Counter.build()
            .name("nocturne_frames_total").help("Nocturne frames total count")
            .labelNames("className")
            .register();

    private static final Summary FRAMES_LATENCY_SECONDS = Summary.build()
            .name("nocturne_frames_latency_seconds").help("Nocturne frames latency in seconds")
            .labelNames("className", "phase")
            .register();

    public static Counter getPagesCounter() {
        return PAGES_COUNTER;
    }

    public static Summary getPagesLatencySeconds() {
        return PAGES_LATENCY_SECONDS;
    }

    public static Counter getFramesCounter() {
        return FRAMES_COUNTER;
    }

    public static Summary getFramesLatencySeconds() {
        return FRAMES_LATENCY_SECONDS;
    }
}
