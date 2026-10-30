package com.agency.dashboard.service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.agency.dashboard.api.model.DashboardResponse;
import com.agency.dashboard.api.model.RegionRow;
import com.agency.dashboard.api.model.StatusGroupMeta;
import com.agency.dashboard.config.DashboardProperties;
import org.springframework.stereotype.Service;

/**
 * Owns the single dashboard snapshot the API exposes.
 *
 * <p>With push (SSE) there is exactly one "current" dataset: it is generated lazily on the
 * first read and only regenerated when someone asks for a refresh, so every client (GET,
 * SSE subscriber, refresh response) sees the same numbers. Previously every GET generated a
 * new dataset, which would make SSE subscribers and REST readers disagree.</p>
 */
@Service
public class DashboardService {

    public static final ZoneId DASHBOARD_ZONE = ZoneId.of("Asia/Bangkok");

    private final AtomicReference<DashboardResponse> snapshot = new AtomicReference<>();
    private final DashboardProperties properties;
    private final DashboardGenerator generator;
    private final OverallAggregator aggregator;

    public DashboardService(DashboardProperties properties, DashboardGenerator generator, OverallAggregator aggregator) {
        this.properties = properties;
        this.generator = generator;
        this.aggregator = aggregator;
    }

    /** Current dataset, generated on first use. */
    public DashboardResponse currentDashboard() {
        return snapshot.updateAndGet(existing -> existing != null ? existing : generate());
    }

    /** Generates a brand new random dataset (same generator as before) and makes it current. */
    public DashboardResponse refreshDashboard() {
        return snapshot.updateAndGet(ignored -> generate());
    }

    private DashboardResponse generate() {
        List<RegionRow> regions = properties.regions().stream().map(generator::generateRegion).toList();
        Map<String, StatusGroupMeta> statusGroups = new LinkedHashMap<>(properties.statusGroups());
        OffsetDateTime updatedAt = OffsetDateTime.now(DASHBOARD_ZONE).truncatedTo(ChronoUnit.SECONDS);
        return new DashboardResponse(updatedAt, statusGroups, regions, aggregator.overall(regions));
    }
}
