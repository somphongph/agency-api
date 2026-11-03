package com.agency.dashboard.api;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void servesTheDashboardContract() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.type").value("success"))
                .andExpect(jsonPath("$.message").value("Successful."))
                .andExpect(jsonPath("$.data.updatedAt").isString())
                .andExpect(jsonPath("$.data.statusGroups.submit.statusCodes").value("Status 00,01,02"))
                .andExpect(jsonPath("$.data.statusGroups.approved.label").value("APPROVED"))
                .andExpect(jsonPath("$.data.regions.length()").value(8))
                .andExpect(jsonPath("$.data.regions[0].region").value("BKK-A"))
                .andExpect(jsonPath("$.data.regions[7].region").value("Other"))
                .andExpect(jsonPath("$.data.regions[0].submit.daily.cases").isNumber())
                .andExpect(jsonPath("$.data.regions[0].pending.ytd.anp").isNumber())
                .andExpect(jsonPath("$.data.overall.region").value("OVERALL"))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode dashboard = root.get("data");
        assertThat(root.get("success").asBoolean()).isTrue();
        assertThat(root.get("type").asString()).isEqualTo("success");
        assertThat(dashboard.get("updatedAt").asString()).endsWith("+07:00");

        JsonNode regions = dashboard.get("regions");
        JsonNode overall = dashboard.get("overall");
        for (String group : new String[] {"submit", "approved", "pending"}) {
            for (String period : new String[] {"daily", "mtd", "ytd"}) {
                int expectedCases = 0;
                long expectedAnpHundredths = 0;
                for (JsonNode region : regions) {
                    expectedCases += region.get(group).get(period).get("cases").asInt();
                    expectedAnpHundredths += hundredths(region.get(group).get(period).get("anp").decimalValue());
                }
                assertThat(overall.get(group).get(period).get("cases").asInt()).isEqualTo(expectedCases);
                assertThat(hundredths(overall.get(group).get(period).get("anp").decimalValue()))
                        .isEqualTo(expectedAnpHundredths);
            }
        }
    }

    @Test
    void keepsTheSameSnapshotUntilARefreshIsRequested() throws Exception {
        String first = dashboardData();
        String second = dashboardData();

        assertThat(second).isEqualTo(first);
    }

    @Test
    void refreshGeneratesNewRandomNumbers() throws Exception {
        String before = dashboardData();

        String afterBody = mockMvc.perform(post("/api/v1/dashboard/refresh"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.type").value("success"))
                .andExpect(jsonPath("$.data.regions.length()").value(8))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode after = objectMapper.readTree(afterBody).get("data");
        assertThat(after.toString()).isNotEqualTo(before);
        assertThat(after.get("regions").get(0).get("submit")).isNotNull();
        assertThat(after.get("overall").get("region").asString()).isEqualTo("OVERALL");

        // The refreshed dataset is now the current one.
        assertThat(dashboardData()).isEqualTo(after.toString());
    }

    private String dashboardData() throws Exception {
        String body = mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).get("data").toString();
    }

    private static long hundredths(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }
}
