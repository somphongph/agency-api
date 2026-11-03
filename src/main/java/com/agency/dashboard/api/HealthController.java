package com.agency.dashboard.api;

import com.agency.dashboard.api.model.ApiResponse;
import com.agency.dashboard.api.model.HealthStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cheap probe for the ALB target group and the ECS container health check.
 *
 * <p>It deliberately does not touch {@link com.agency.dashboard.service.DashboardService}: a
 * health check runs every few seconds and must never generate data, push SSE events or write
 * to the log. Use {@code /api/v1/dashboard} for the payload itself.</p>
 */
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "health", description = "Liveness probe for the load balancer and ECS")
public class HealthController {

    private static final HealthStatus UP = new HealthStatus("UP");

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Health check",
            description = "Returns UP in the standard envelope. Constant work: no data generation, "
                    + "no SSE broadcast, safe to call every few seconds.")
    public ResponseEntity<ApiResponse<HealthStatus>> health() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(UP));
    }
}
