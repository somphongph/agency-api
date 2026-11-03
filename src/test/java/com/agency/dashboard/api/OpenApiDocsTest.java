package com.agency.dashboard.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The spec is generated from the code, so this test is the contract that the
 * documented endpoints and schemas stay in sync with the controllers.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publishesDocumentMetadata() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value(containsString("3.")))
                .andExpect(jsonPath("$.info.title").value("agency-api"))
                .andExpect(jsonPath("$.info.version").value("1.0.0"))
                .andExpect(jsonPath("$.info.description").value("Agency Dashboard API"))
                .andExpect(jsonPath("$.info.license.name").value("Apache 2.0"));
    }

    @Test
    void documentsTheDashboardEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/api/v1/dashboard'].get.tags[0]").value("dashboard"))
                .andExpect(jsonPath("$['paths']['/api/v1/dashboard'].get.summary").value("Get the current dashboard"))
                .andExpect(jsonPath("$['paths']['/api/v1/dashboard'].get.responses['200']").exists())
                .andExpect(jsonPath("$['paths']['/api/v1/dashboard'].get.responses['200'].content"
                        + "['application/json'].schema['$ref']").value("#/components/schemas/ApiResponseDashboardResponse"));
    }

    @Test
    void documentsTheRefreshAndStreamEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/api/v1/dashboard/refresh'].post.summary")
                        .value("Push a new random dataset"))
                .andExpect(jsonPath("$['paths']['/api/v1/dashboard/stream'].get.summary")
                        .value("Stream dashboard updates (SSE)"))
                .andExpect(jsonPath("$['paths']['/api/v1/dashboard/stream'].get.responses['200'].content"
                        + "['text/event-stream']").exists());
    }

    @Test
    void documentsTheResponseEnvelope() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.ApiResponseDashboardResponse.properties.success.type")
                        .value("boolean"))
                .andExpect(jsonPath("$.components.schemas.ApiResponseDashboardResponse.properties.success.example")
                        .value(true))
                .andExpect(jsonPath("$.components.schemas.ApiResponseDashboardResponse.properties.type.example")
                        .value("success"))
                .andExpect(jsonPath("$.components.schemas.ApiResponseDashboardResponse.properties.message.example")
                        .value("Successful."))
                .andExpect(jsonPath("$.components.schemas.ApiResponseDashboardResponse.properties.data['$ref']")
                        .value("#/components/schemas/DashboardResponse"));
    }

    @Test
    void documentsTheResponseSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.DashboardResponse.properties.updatedAt.type").value("string"))
                .andExpect(jsonPath("$.components.schemas.DashboardResponse.properties.updatedAt.format")
                        .value("date-time"))
                .andExpect(jsonPath("$.components.schemas.DashboardResponse.properties.regions.type").value("array"))
                .andExpect(jsonPath("$.components.schemas.Metric.properties.anp.type").value("number"))
                .andExpect(jsonPath("$.components.schemas.Metric.properties.anp.example").value(12.34))
                .andExpect(jsonPath("$.components.schemas.Metric.properties.cases.example").value(4210))
                .andExpect(jsonPath("$.components.schemas.RegionRow.properties.region.example").value("BKK-A"))
                .andExpect(jsonPath("$.components.schemas.StatusGroupMeta.properties.statusCodes.example")
                        .value("Status 00,01,02"));
    }

    @Test
    void servesSwaggerUi() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", endsWith("/swagger-ui/index.html")));
    }
}
