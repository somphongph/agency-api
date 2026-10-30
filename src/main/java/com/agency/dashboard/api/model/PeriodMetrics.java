package com.agency.dashboard.api.model;

/**
 * Daily / MTD / YTD metrics for a single status group.
 */
public record PeriodMetrics(Metric daily, Metric mtd, Metric ytd) {
}
