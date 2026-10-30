package com.agency.dashboard.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;

import com.agency.dashboard.api.model.Metric;
import com.agency.dashboard.api.model.PeriodMetrics;
import com.agency.dashboard.api.model.RegionRow;
import org.springframework.stereotype.Component;

/**
 * Sums region rows into the OVERALL row.
 *
 * <p>Each ANP value is already exactly two decimals, so the sum of the values shown on
 * screen equals the OVERALL value cell by cell — no extra rounding drift.</p>
 */
@Component
public class OverallAggregator {

    public RegionRow overall(List<RegionRow> regions) {
        return new RegionRow(
                "OVERALL",
                sum(regions, RegionRow::submit),
                sum(regions, RegionRow::approved),
                sum(regions, RegionRow::pending));
    }

    private static PeriodMetrics sum(List<RegionRow> regions, Function<RegionRow, PeriodMetrics> selector) {
        List<PeriodMetrics> metrics = regions.stream().map(selector).toList();
        return new PeriodMetrics(
                sumPeriod(metrics, PeriodMetrics::daily),
                sumPeriod(metrics, PeriodMetrics::mtd),
                sumPeriod(metrics, PeriodMetrics::ytd));
    }

    private static Metric sumPeriod(List<PeriodMetrics> metrics, Function<PeriodMetrics, Metric> selector) {
        int cases = metrics.stream().map(selector).mapToInt(Metric::cases).sum();
        long anpHundredths = metrics.stream().map(selector).mapToLong(metric -> toHundredths(metric.anp())).sum();
        return new Metric(cases, BigDecimal.valueOf(anpHundredths, 2));
    }

    private static long toHundredths(BigDecimal anp) {
        return anp.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }
}
