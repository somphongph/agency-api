package com.agency.dashboard.api.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Payload of GET /api/v1/dashboard.
 *
 * <p>{@code overall} is calculated by the backend (see {@code OverallAggregator}) so that
 * every client receives exactly the same totals and the rounding rule lives in one place.</p>
 */
public record DashboardResponse(
        OffsetDateTime updatedAt,
        Map<String, StatusGroupMeta> statusGroups,
        List<RegionRow> regions,
        RegionRow overall) {
}
