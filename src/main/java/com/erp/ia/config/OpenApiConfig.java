package com.erp.ia.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger configuration.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI erpOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ERP IA-First API")
                        .version("0.1.0")
                        .description("Sistema ERP com IA como cérebro decisório. "
                                + "Agentes analisam dados e sugerem ações, que são "
                                + "aprovadas e executadas de forma auditável.")
                        .contact(new Contact().name("Lucas")));
    }
}
