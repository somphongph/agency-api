package com.agency.dashboard.service;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

import com.agency.dashboard.api.model.Metric;
import com.agency.dashboard.api.model.PeriodMetrics;
import com.agency.dashboard.api.model.RegionRow;
import com.agency.dashboard.config.DashboardProperties;
import org.springframework.stereotype.Component;

/**
 * Creates a fresh random dataset on every call. No database, no caching.
 *
 * <p>ANP is generated from integer "hundredths of mb" so that values are exact at two
 * decimal places. That makes {@code overall} an exact sum of the displayed per-region
 * values (see {@link OverallAggregator}).</p>
 */
@Component
public class DashboardGenerator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final DashboardProperties properties;

    public DashboardGenerator(DashboardProperties properties) {
        this.properties = properties;
    }

    public RegionRow generateRegion(String region) {
        return new RegionRow(region, generateStatus("submit"), generateStatus("approved"), generateStatus("pending"));
    }

    private PeriodMetrics generateStatus(String statusKey) {
        DashboardProperties.CaseRange range = properties.caseRanges().get(statusKey);
        if (range == null) {
            throw new IllegalStateException("Missing dashboard.case-ranges." + statusKey);
        }

        int ytdCases = randomInt(range.min(), range.max());
        int mtdCases = randomInt(0, share(ytdCases, properties.anp().mtdMaxShare()));
        int dailyCases = randomInt(0, share(mtdCases, properties.anp().dailyMaxShare()));

        // One rate per region + status keeps daily <= mtd <= ytd true for ANP as well.
        long rateHundredths = randomLong(hundredths(properties.anp().rateMin()), hundredths(properties.anp().rateMax()));

        return new PeriodMetrics(
                metric(dailyCases, rateHundredths),
                metric(mtdCases, rateHundredths),
                metric(ytdCases, rateHundredths));
    }

    private static Metric metric(int cases, long rateHundredths) {
        return new Metric(cases, BigDecimal.valueOf(cases * rateHundredths, 2));
    }

    private static int share(int total, double maxShare) {
        return (int) Math.floor(total * maxShare);
    }

    private static long hundredths(BigDecimal value) {
        return value.multiply(HUNDRED).longValueExact();
    }

    private static int randomInt(int minInclusive, int maxInclusive) {
        if (maxInclusive <= minInclusive) {
            return minInclusive;
        }
        return ThreadLocalRandom.current().nextInt(minInclusive, maxInclusive + 1);
    }

    private static long randomLong(long minInclusive, long maxInclusive) {
        if (maxInclusive <= minInclusive) {
            return minInclusive;
        }
        return ThreadLocalRandom.current().nextLong(minInclusive, maxInclusive + 1);
    }
}
