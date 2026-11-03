package com.agency.dashboard.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Static document metadata, the equivalent of the {@code // @title / @version / @contact}
 * block that annotates the router in tgr-api-place.
 *
 * <p>Everything else (paths, parameters, schemas) is generated at runtime by springdoc from
 * the controllers and the API model records, so there is no {@code docs/} folder to keep in
 * sync: the spec is always the code.</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI agencyApiOpenApi() {
        Info info = new Info()
                .title("agency-api")
                .version("1.0.0")
                .description("Agency Dashboard API")
                .contact(new Contact().name("API Support"))
                .license(new License()
                        .name("Apache 2.0")
                        .url("http://www.apache.org/licenses/LICENSE-2.0.html"));

        // Relative, so the spec also works when served through the Angular dev-server proxy.
        Server server = new Server().url("/").description("Current host");

        return new OpenAPI().info(info).addServersItem(server);
    }
}
