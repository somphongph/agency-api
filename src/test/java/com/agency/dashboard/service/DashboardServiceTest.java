package com.agency.dashboard.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.agency.dashboard.api.model.DashboardResponse;
import com.agency.dashboard.api.model.Metric;
import com.agency.dashboard.api.model.PeriodMetrics;
import com.agency.dashboard.api.model.RegionRow;
import com.agency.dashboard.api.model.StatusGroupMeta;
import com.agency.dashboard.config.DashboardProperties;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardServiceTest {

    private static final List<String> REGIONS = List.of(
            "BKK-A", "BKK-B", "Central", "West", "South", "North", "Northeast", "Other");

    private final DashboardService service = new DashboardService(properties(), new DashboardGenerator(properties()), new OverallAggregator());

    @Test
    void returnsEveryRegionInOrderAndTheConfiguredStatusGroups() {
        DashboardResponse response = service.currentDashboard();

        assertThat(response.regions()).extracting(RegionRow::region).containsExactlyElementsOf(REGIONS);
        assertThat(response.overall().region()).isEqualTo("OVERALL");
        assertThat(response.statusGroups()).containsOnlyKeys("submit", "approved", "pending");
        assertThat(response.statusGroups().get("pending"))
                .isEqualTo(new StatusGroupMeta("PENDING", "Status 20/21/29/30/31/33"));
        assertThat(response.updatedAt().getOffset().getTotalSeconds()).isEqualTo(7 * 3600);
    }

    @RepeatedTest(100)
    void everyDatasetKeepsTheBusinessRules() {
        DashboardResponse response = service.currentDashboard();

        response.regions().forEach(region -> {
            assertStatus(region.submit());
            assertStatus(region.approved());
            assertStatus(region.pending());
        });

        assertOverallEqualsSum(response.overall(), response.regions());
    }

    @Test
    void keepsTheSameSnapshotUntilTheDashboardIsRefreshed() {
        DashboardResponse first = service.currentDashboard();
        DashboardResponse second = service.currentDashboard();

        assertThat(second).isSameAs(first);
    }

    @RepeatedTest(20)
    void refreshGeneratesFreshRandomData() {
        DashboardResponse first = service.currentDashboard();
        DashboardResponse refreshed = service.refreshDashboard();

        assertThat(snapshot(refreshed)).isNotEqualTo(snapshot(first));
        assertThat(service.currentDashboard()).isSameAs(refreshed);
    }

    private static void assertStatus(PeriodMetrics metrics) {
        assertThat(metrics.daily().cases()).isGreaterThanOrEqualTo(0);
        assertThat(metrics.mtd().cases()).isGreaterThanOrEqualTo(metrics.daily().cases());
        assertThat(metrics.ytd().cases()).isGreaterThanOrEqualTo(metrics.mtd().cases());

        for (Metric metric : List.of(metrics.daily(), metrics.mtd(), metrics.ytd())) {
            assertThat(metric.anp()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
            assertThat(metric.anp().scale()).isEqualTo(2);
        }

        assertThat(metrics.mtd().anp()).isGreaterThanOrEqualTo(metrics.daily().anp());
        assertThat(metrics.ytd().anp()).isGreaterThanOrEqualTo(metrics.mtd().anp());
    }

    private static void assertOverallEqualsSum(RegionRow overall, List<RegionRow> regions) {
        assertThat(overall.submit()).isEqualTo(sum(regions, RegionRow::submit));
        assertThat(overall.approved()).isEqualTo(sum(regions, RegionRow::approved));
        assertThat(overall.pending()).isEqualTo(sum(regions, RegionRow::pending));
    }

    private static PeriodMetrics sum(List<RegionRow> regions, Function<RegionRow, PeriodMetrics> selector) {
        List<PeriodMetrics> list = regions.stream().map(selector).toList();
        return new PeriodMetrics(
                sumMetric(list, PeriodMetrics::daily),
                sumMetric(list, PeriodMetrics::mtd),
                sumMetric(list, PeriodMetrics::ytd));
    }

    private static Metric sumMetric(List<PeriodMetrics> list, Function<PeriodMetrics, Metric> selector) {
        int cases = list.stream().map(selector).mapToInt(Metric::cases).sum();
        long anp = list.stream().map(selector).mapToLong(metric -> hundredths(metric.anp())).sum();
        return new Metric(cases, BigDecimal.valueOf(anp, 2));
    }

    private static long hundredths(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    private static String snapshot(DashboardResponse response) {
        return response.regions().stream()
                .map(region -> region.region() + "|" + region.submit() + region.approved() + region.pending())
                .collect(Collectors.joining(";"));
    }

    private static DashboardProperties properties() {
        Map<String, DashboardProperties.CaseRange> ranges = new LinkedHashMap<>();
        ranges.put("submit", new DashboardProperties.CaseRange(1200, 9000));
        ranges.put("approved", new DashboardProperties.CaseRange(900, 7000));
        ranges.put("pending", new DashboardProperties.CaseRange(80, 1200));

        Map<String, StatusGroupMeta> groups = new LinkedHashMap<>();
        groups.put("submit", new StatusGroupMeta("SUBMIT", "Status 00,01,02"));
        groups.put("approved", new StatusGroupMeta("APPROVED", "Status 40"));
        groups.put("pending", new StatusGroupMeta("PENDING", "Status 20/21/29/30/31/33"));

        return new DashboardProperties(
                REGIONS,
                groups,
                ranges,
                new DashboardProperties.Anp(new BigDecimal("4.50"), new BigDecimal("28.00"), 0.15, 0.20));
    }
}
