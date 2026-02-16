package com.inventory.reporting.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI reportingServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Reporting Service API")
                        .description("Real-time inventory reports and event history")
                        .version("1.0.0"));
    }
}
