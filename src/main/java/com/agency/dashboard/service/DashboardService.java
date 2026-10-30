package com.agency.dashboard.service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.agency.dashboard.api.model.DashboardResponse;
import com.agency.dashboard.api.model.RegionRow;
import com.agency.dashboard.api.model.StatusGroupMeta;
import com.agency.dashboard.config.DashboardProperties;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    public static final ZoneId DASHBOARD_ZONE = ZoneId.of("Asia/Bangkok");

    private final DashboardProperties properties;
    private final DashboardGenerator generator;
    private final OverallAggregator aggregator;

    public DashboardService(DashboardProperties properties, DashboardGenerator generator, OverallAggregator aggregator) {
        this.properties = properties;
        this.generator = generator;
        this.aggregator = aggregator;
    }

    public DashboardResponse currentDashboard() {
        List<RegionRow> regions = properties.regions().stream().map(generator::generateRegion).toList();
        Map<String, StatusGroupMeta> statusGroups = new LinkedHashMap<>(properties.statusGroups());
        OffsetDateTime updatedAt = OffsetDateTime.now(DASHBOARD_ZONE).truncatedTo(ChronoUnit.SECONDS);
        return new DashboardResponse(updatedAt, statusGroups, regions, aggregator.overall(regions));
    }
}
