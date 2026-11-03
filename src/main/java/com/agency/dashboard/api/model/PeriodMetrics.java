package com.agency.dashboard.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Daily / MTD / YTD metrics for a single status group.
 */
@Schema(description = "Daily / MTD / YTD metrics for a single status group")
public record PeriodMetrics(
        @Schema(description = "Today's numbers")
        Metric daily,
        @Schema(description = "Month-to-date numbers")
        Metric mtd,
        @Schema(description = "Year-to-date numbers")
        Metric ytd) {
}
