package com.agency.dashboard.config;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.agency.dashboard.api.model.StatusGroupMeta;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * All mock-data knobs live in application.yml so ranges can be tuned without touching code.
 */
@ConfigurationProperties(prefix = "dashboard")
public record DashboardProperties(
        List<String> regions,
        Map<String, StatusGroupMeta> statusGroups,
        Map<String, CaseRange> caseRanges,
        Anp anp) {

    /**
     * Inclusive range for the YTD case count of one region + status group.
     * MTD and daily are derived as a share of the next larger period so that
     * daily <= mtd <= ytd always holds.
     */
    public record CaseRange(int min, int max) {
    }

    /**
     * @param rateMin          minimum ANP per case in mb
     * @param rateMax          maximum ANP per case in mb
     * @param mtdMaxShare      MTD is at most this share of YTD
     * @param dailyMaxShare    daily is at most this share of MTD
     */
    public record Anp(
            BigDecimal rateMin,
            BigDecimal rateMax,
            double mtdMaxShare,
            double dailyMaxShare) {
    }
}
