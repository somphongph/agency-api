package com.agency.dashboard.api;

import com.agency.dashboard.api.model.ApiResponse;
import com.agency.dashboard.api.model.DashboardResponse;
import com.agency.dashboard.service.DashboardService;
import com.agency.dashboard.service.DashboardStream;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "dashboard", description = "Agency dashboard aggregates")
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardStream dashboardStream;

    public DashboardController(DashboardService dashboardService, DashboardStream dashboardStream) {
        this.dashboardService = dashboardService;
        this.dashboardStream = dashboardStream;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get the current dashboard",
            description = "Returns the current dashboard snapshot inside the standard envelope "
                    + "(success / type / message / data): per-region and OVERALL metrics for the "
                    + "submit / approved / pending status groups over daily, MTD and YTD periods. "
                    + "The numbers only change when the refresh endpoint is called; use the stream "
                    + "endpoint to be notified. Errors use the same envelope with success=false, a "
                    + "type/message describing the failure, and data=null.")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        return ok(dashboardService.currentDashboard());
    }

    @PostMapping(path = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Push a new random dataset",
            description = "Generates a new random dataset with the configured ranges, makes it current "
                    + "and pushes it to every subscriber of the stream endpoint. The new snapshot is "
                    + "also returned in the response envelope.")
    public ResponseEntity<ApiResponse<DashboardResponse>> refreshDashboard() {
        DashboardResponse snapshot = dashboardService.refreshDashboard();
        dashboardStream.publish(snapshot);
        return ok(snapshot);
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Stream dashboard updates (SSE)",
            description = "Server-Sent Events stream. The current snapshot is sent immediately under the "
                    + "event name `dashboard` (same envelope as the JSON endpoints), and every refresh "
                    + "request pushes a new event to all connected browsers. A comment line is sent every "
                    + "20 seconds to keep proxies from closing the connection.")
    public SseEmitter stream() {
        return dashboardStream.subscribe();
    }

    private static ResponseEntity<ApiResponse<DashboardResponse>> ok(DashboardResponse snapshot) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(snapshot));
    }
}
