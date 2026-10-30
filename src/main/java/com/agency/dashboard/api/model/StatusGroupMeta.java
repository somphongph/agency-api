package com.agency.dashboard.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Header metadata for a status group, e.g. SUBMIT / "Status 00,01,02".
 */
@Schema(description = "Header metadata for a status group, e.g. SUBMIT / \"Status 00,01,02\"")
public record StatusGroupMeta(
        @Schema(description = "Label shown in the dashboard header", example = "SUBMIT")
        String label,
        @Schema(description = "Comma-separated status codes covered by this group", example = "Status 00,01,02")
        String statusCodes) {
}
