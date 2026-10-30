package com.agency.dashboard.api.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Payload of GET /api/v1/dashboard.
 *
 * <p>{@code overall} is calculated by the backend (see {@code OverallAggregator}) so that
 * every client receives exactly the same totals and the rounding rule lives in one place.</p>
 */
@Schema(description = "Dashboard payload: one row per region plus the pre-computed OVERALL row")
public record DashboardResponse(
        @Schema(description = "Generation time in +07:00", example = "2026-10-30T09:15:00+07:00")
        OffsetDateTime updatedAt,
        @Schema(description = "Header metadata per status group, keyed by group name")
        Map<String, StatusGroupMeta> statusGroups,
        @Schema(description = "One row per region, in configured order")
        List<RegionRow> regions,
        @Schema(description = "Totals across all regions")
        RegionRow overall) {

    public DashboardResponse {
        statusGroups = Map.copyOf(statusGroups);
        regions = List.copyOf(regions);
    }
}
