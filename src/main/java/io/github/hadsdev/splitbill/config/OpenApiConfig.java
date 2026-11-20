package io.github.hadsdev.splitbill.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI configuration for the Split Bill API.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI splitBillOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Split Bill API")
                        .description("REST API for managing bill groups, expenses, and settlements.")
                        .version("v1.0.0"));
    }
}
