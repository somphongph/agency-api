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
                .andExpect(jsonPath("$.updatedAt").isString())
                .andExpect(jsonPath("$.statusGroups.submit.statusCodes").value("Status 00,01,02"))
                .andExpect(jsonPath("$.statusGroups.approved.label").value("APPROVED"))
                .andExpect(jsonPath("$.regions.length()").value(8))
                .andExpect(jsonPath("$.regions[0].region").value("BKK-A"))
                .andExpect(jsonPath("$.regions[7].region").value("Other"))
                .andExpect(jsonPath("$.regions[0].submit.daily.cases").isNumber())
                .andExpect(jsonPath("$.regions[0].pending.ytd.anp").isNumber())
                .andExpect(jsonPath("$.overall.region").value("OVERALL"))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(root.get("updatedAt").asString()).endsWith("+07:00");

        JsonNode regions = root.get("regions");
        JsonNode overall = root.get("overall");
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
    void returnsNewNumbersOnEveryRequest() throws Exception {
        String first = payload();
        String second = payload();

        assertThat(second).isNotEqualTo(first);
    }

    private String payload() throws Exception {
        return mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private static long hundredths(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }
}
