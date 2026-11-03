package com.agency.dashboard.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The prod profile hides the docs, like tgr-api-place only mounts Swagger when env != prod.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class OpenApiDocsDisabledInProdTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void doesNotPublishTheSpec() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isNotFound());
    }

    @Test
    void stillServesTheApi() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard")).andExpect(status().isOk());
    }
}
