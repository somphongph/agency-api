package com.agency.dashboard.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Envelope used by every endpoint, mirroring {@code xres.SuccessResponse[T]} in
 * tgr-api-place so that clients parse one shape for both success and failure.
 *
 * @param success true on 2xx
 * @param type    machine-readable outcome, e.g. {@code success} or {@code not_found}
 * @param message human-readable message
 * @param data    endpoint payload, {@code null} on error
 */
@Schema(description = "Response envelope: success/type/message plus the payload in data")
public record ApiResponse<T>(
        @Schema(description = "True on success", example = "true")
        boolean success,
        @Schema(description = "Machine-readable outcome", example = "success")
        String type,
        @Schema(description = "Human-readable message", example = "Successful.")
        String message,
        @Schema(description = "Endpoint payload; null on error")
        T data) {

    public static final String TYPE_SUCCESS = "success";
    public static final String MESSAGE_SUCCESS = "Successful.";

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, TYPE_SUCCESS, MESSAGE_SUCCESS, data);
    }

    public static <T> ApiResponse<T> error(String type, String message) {
        return new ApiResponse<>(false, type, message, null);
    }
}
