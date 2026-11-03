package com.agency.dashboard.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The Prometheus endpoint is what the whitebox dashboard scrapes, so the metric names the
 * dashboard queries must actually exist.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MetricsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesJvmHttpAndCustomSseMetrics() throws Exception {
        // Generate some traffic so http_server_requests_seconds exists.
        mockMvc.perform(get("/api/v1/health")).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/dashboard/refresh")).andExpect(status().isOk());

        String scrape = mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("jvm_memory_used_bytes")))
                .andExpect(content().string(containsString("http_server_requests_seconds_count")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(scrape)
                .contains("jvm_memory_used_bytes")
                .contains("jvm_threads_live_threads")
                .contains("process_cpu_usage")
                .contains("http_server_requests_seconds_count")
                .contains("dashboard_stream_subscribers")
                .contains("dashboard_refresh_total")
                .contains("dashboard_snapshot_timestamp_seconds")
                .contains("application=\"agency-api\"");
    }
}
