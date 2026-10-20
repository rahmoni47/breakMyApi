package com.openspec.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Springdoc OpenAPI configuration for openSpec backend API contract and Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openSpecOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("openSpec Backend API")
                        .description("""
                                # openSpec Backend API Contract

                                Welcome to the openSpec Backend REST API documentation.
                                This API contract provides all endpoint definitions, request/response models,
                                Server-Sent Event (SSE) contracts, enums, and error shapes required for the frontend
                                integration of the openSpec automated API testing suite.

                                ### Key Architecture Rules:
                                - **Test Run is primary**: Every test run represents an execution against a given OpenAPI document.
                                - **Asynchronous Workflow**: Test runs are created with HTTP `202 Accepted` and real-time execution progress is tracked via SSE.
                                - **AI Configuration**: Dedicated endpoints to configure LLM providers. Secrets are never exposed.
                                - **Artifacts**: Markdown reports (`text/markdown`) and raw log files (`text/plain`) have dedicated endpoints and download links.
                                - **Contract Only**: This phase establishes the complete API contract; backend execution stubs provide realistic sample data.
                                """)
                        .version("0.1.0")
                        .contact(new Contact()
                                .name("openSpec Development Team")
                                .email("team@openspec.dev"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .tags(List.of(
                        new Tag().name("AI Configuration").description("Endpoints for configuring and checking AI/LLM provider settings"),
                        new Tag().name("Test Run Management").description("Endpoints for OpenAPI validation, test run creation, SSE stream, history, and lifecycle control")
                ))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8081")
                                .description("Local Development Server")
                ));
    }
}
