package com.agency.dashboard.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.agency.dashboard.api.model.ApiResponse;
import com.agency.dashboard.api.model.DashboardResponse;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Keeps the open Server-Sent Events connections and pushes the dashboard to all of them.
 *
 * <p>Every event carries the same {@link ApiResponse} envelope as the REST endpoints, under
 * the event name {@code dashboard}. A periodic comment keeps proxies (ALB, CloudFront) from
 * closing an idle stream. The heartbeat thread is a daemon so it can never keep the JVM alive.</p>
 */
@Component
public class DashboardStream {

    private static final Logger log = LoggerFactory.getLogger(DashboardStream.class);

    static final long HEARTBEAT_MS = 20_000;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "dashboard-heartbeat");
        thread.setDaemon(true);
        return thread;
    });
    private final DashboardService dashboardService;

    public DashboardStream(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
        heartbeatExecutor.scheduleAtFixedRate(this::heartbeat, HEARTBEAT_MS, HEARTBEAT_MS, TimeUnit.MILLISECONDS);
    }

    /** Registers a browser; the current snapshot is sent immediately so a new tab is never empty. */
    public SseEmitter subscribe() {
        // 0 = never time out; the stream lives until the client or the server closes it.
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            emitter.complete();
        });
        emitter.onError(error -> emitters.remove(emitter));

        send(emitter, dashboardService.currentDashboard());
        return emitter;
    }

    /** Pushes one snapshot to every subscriber. */
    public void publish(DashboardResponse snapshot) {
        emitters.forEach(emitter -> send(emitter, snapshot));
    }

    void heartbeat() {
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (IOException | IllegalStateException ex) {
                // Client vanished between heartbeats; drop it quietly.
                emitters.remove(emitter);
            }
        });
    }

    private void send(SseEmitter emitter, DashboardResponse snapshot) {
        try {
            // Spring MVC serializes the record with the configured message converters.
            emitter.send(SseEmitter.event()
                    .name("dashboard")
                    .data(ApiResponse.ok(snapshot), MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException ex) {
            log.debug("Dropping closed dashboard stream", ex);
            emitters.remove(emitter);
            emitter.complete();
        }
    }

    /** Closes every stream so Tomcat can release the async requests during shutdown. */
    @PreDestroy
    void shutdown() {
        heartbeatExecutor.shutdownNow();
        closeAll();
    }

    /**
     * Runs before the web server starts its graceful shutdown; otherwise the still-open SSE
     * requests count as "active requests" and every shutdown waits for the graceful period.
     */
    @EventListener(ContextClosedEvent.class)
    void onContextClosed() {
        closeAll();
    }

    private void closeAll() {
        emitters.forEach(SseEmitter::complete);
        emitters.clear();
    }
}
