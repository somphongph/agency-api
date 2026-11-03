package com.agency.dashboard.api.model;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One cell of the dashboard.
 *
 * @param cases non-negative number of cases
 * @param anp   ANP in mb, always scaled to exactly two decimal places
 */
@Schema(description = "One dashboard cell: case count and ANP")
public record Metric(
        @Schema(description = "Non-negative number of cases", example = "4210")
        int cases,
        @Schema(description = "ANP in mb, always scaled to exactly two decimal places", example = "12.34")
        BigDecimal anp) {
}
