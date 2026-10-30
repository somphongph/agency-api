package com.agency.dashboard.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Iterator;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The stream is the whole point of the feature, so it is exercised over a real HTTP server
 * instead of MockMvc: subscribe, read the snapshot, trigger a refresh, read the pushed one.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DashboardStreamTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @LocalServerPort
    private int port;

    @Test
    @Timeout(30)
    void sendsTheCurrentSnapshotAndPushesRefreshEvents() throws Exception {
        // HttpClient keeps non-daemon threads, so it is closed with the test.
        try (HttpClient client = newClient()) {
            HttpRequest streamRequest = HttpRequest.newBuilder(uri("/api/v1/dashboard/stream"))
                    .header("Accept", MediaType.TEXT_EVENT_STREAM_VALUE)
                    .timeout(TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<Stream<String>> response = client.send(streamRequest, HttpResponse.BodyHandlers.ofLines());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                    contentType -> assertThat(contentType).startsWith("text/event-stream"));

            try (Stream<String> lines = response.body()) {
                Iterator<String> iterator = lines.iterator();

                Event first = nextEvent(iterator);
                assertThat(first.name).isEqualTo("dashboard");
                JsonNode firstEnvelope = MAPPER.readTree(first.data);
                assertThat(firstEnvelope.get("success").asBoolean()).isTrue();
                assertThat(firstEnvelope.get("type").asString()).isEqualTo("success");
                assertThat(firstEnvelope.get("data").get("regions").size()).isEqualTo(8);
                String firstSnapshot = firstEnvelope.get("data").toString();

                HttpResponse<String> refresh = client.send(
                        HttpRequest.newBuilder(uri("/api/v1/dashboard/refresh"))
                                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                                .timeout(TIMEOUT)
                                .POST(HttpRequest.BodyPublishers.noBody())
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
                assertThat(refresh.statusCode()).isEqualTo(200);

                Event pushed = nextEvent(iterator);
                assertThat(pushed.name).isEqualTo("dashboard");
                assertThat(MAPPER.readTree(pushed.data).get("data").toString())
                        .as("the refresh is pushed to the open stream")
                        .isNotEqualTo(firstSnapshot)
                        .isEqualTo(MAPPER.readTree(refresh.body()).get("data").toString());
            }
        }
    }

    @Test
    @Timeout(30)
    void advertisesTheRefreshEndpointInTheSpec() throws Exception {
        try (HttpClient client = newClient()) {
            HttpResponse<String> spec = client.send(
                    HttpRequest.newBuilder(uri("/v3/api-docs")).timeout(TIMEOUT).GET().build(),
                    HttpResponse.BodyHandlers.ofString());

            assertThat(spec.statusCode()).isEqualTo(200);
            JsonNode paths = MAPPER.readTree(spec.body()).get("paths");
            assertThat(paths.get("/api/v1/dashboard/refresh").has("post")).isTrue();
            assertThat(paths.get("/api/v1/dashboard/stream").has("get")).isTrue();
        }
    }

    private static HttpClient newClient() {
        return HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    /** Skips heartbeat comments and blank separators until the next `data:` line. */
    private static Event nextEvent(Iterator<String> lines) throws IOException {
        String name = "message";
        while (lines.hasNext()) {
            String line = lines.next();
            if (line.startsWith("event:")) {
                name = line.substring("event:".length()).trim();
            } else if (line.startsWith("data:")) {
                return new Event(name, line.substring("data:".length()).trim());
            }
        }
        throw new IOException("The stream ended before another event arrived");
    }

    private record Event(String name, String data) {
    }
}
