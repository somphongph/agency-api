package com.agency.dashboard.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A dashboard row. The same shape is used for every region and for the OVERALL row.
 */
@Schema(description = "A dashboard row; the same shape is used for every region and for OVERALL")
public record RegionRow(
        @Schema(description = "Region name, or OVERALL for the totals row", example = "BKK-A")
        String region,
        PeriodMetrics submit,
        PeriodMetrics approved,
        PeriodMetrics pending) {
}
