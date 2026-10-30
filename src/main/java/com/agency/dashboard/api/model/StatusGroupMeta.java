package com.agency.dashboard.api.model;

/**
 * Header metadata for a status group, e.g. SUBMIT / "Status 00,01,02".
 */
public record StatusGroupMeta(String label, String statusCodes) {
}
