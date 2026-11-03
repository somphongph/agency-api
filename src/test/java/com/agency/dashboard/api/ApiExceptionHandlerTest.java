package com.agency.dashboard.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Errors must use the same envelope as successes, without leaking internals.
 */
class ApiExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ThrowingController())
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    @Test
    void wrapsUnexpectedErrorsAsInternalError() throws Exception {
        String body = mockMvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.type").value("internal_error"))
                .andExpect(jsonPath("$.message").value("Internal server error."))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain("super secret detail");
    }

    @Test
    void keepsMvcStatusCodesInsteadOfTurningThemInto500() throws Exception {
        mockMvc.perform(get("/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.type").value("not_found"))
                .andExpect(jsonPath("$.message").value("Not Found."));
    }

    @Test
    void doesNotLeakTheReasonOfAServerSideStatusException() throws Exception {
        String body = mockMvc.perform(get("/upstream"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.type").value("internal_error"))
                .andExpect(jsonPath("$.message").value("Internal server error."))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain("10.0.0.7");
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/boom")
        String boom() {
            throw new IllegalStateException("super secret detail");
        }

        @GetMapping("/missing")
        String missing() {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        @GetMapping("/upstream")
        String upstream() {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "upstream refused at 10.0.0.7");
        }
    }
}
