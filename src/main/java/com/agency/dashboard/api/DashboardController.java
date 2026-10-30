package com.agency.dashboard.api;

import com.agency.dashboard.api.model.ApiResponse;
import com.agency.dashboard.api.model.DashboardResponse;
import com.agency.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "dashboard", description = "Agency dashboard aggregates")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get the dashboard",
            description = "Returns the dashboard payload inside the standard envelope "
                    + "(success / type / message / data): per-region and OVERALL metrics for the "
                    + "submit / approved / pending status groups over daily, MTD and YTD periods. "
                    + "The data is generated per request, so two calls never return the same numbers. "
                    + "Errors use the same envelope with success=false, a type/message describing the "
                    + "failure, and data=null.")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(dashboardService.currentDashboard()));
    }
}
