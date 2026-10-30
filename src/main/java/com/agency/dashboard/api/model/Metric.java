package com.agency.dashboard.api.model;

import java.math.BigDecimal;

/**
 * One cell of the dashboard.
 *
 * @param cases non-negative number of cases
 * @param anp   ANP in mb, always scaled to exactly two decimal places
 */
public record Metric(int cases, BigDecimal anp) {
}
