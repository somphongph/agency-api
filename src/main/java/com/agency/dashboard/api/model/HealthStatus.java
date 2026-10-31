package com.agency.dashboard.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Body of the health endpoint, wrapped in the standard envelope.
 */
@Schema(description = "Service health")
public record HealthStatus(
        @Schema(description = "UP while the API can serve requests", example = "UP")
        String status) {
}
