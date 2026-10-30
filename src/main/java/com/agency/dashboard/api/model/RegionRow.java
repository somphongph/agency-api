package com.agency.dashboard.api.model;

/**
 * A dashboard row. The same shape is used for every region and for the OVERALL row.
 */
public record RegionRow(
        String region,
        PeriodMetrics submit,
        PeriodMetrics approved,
        PeriodMetrics pending) {
}
