package com.openspec.core.openapi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenApiDocumentValidatorTests {

    private final OpenApiDocumentValidator validator = new OpenApiDocumentValidator();

    private static final String VALID_OPENAPI_JSON = """
            {
              "openapi": "3.0.3",
              "info": {
                "title": "Payment Gateway API",
                "version": "1.0.0"
              },
              "servers": [
                {
                  "url": "https://api.payments.com/v1"
                }
              ],
              "paths": {
                "/charges": {
                  "post": {
                    "summary": "Create a charge",
                    "operationId": "createCharge",
                    "responses": {
                      "201": {
                        "description": "Created",
                        "content": {
                          "application/json": {}
                        }
                      },
                      "400": {
                        "description": "Invalid charge payload"
                      }
                    }
                  }
                },
                "/charges/{id}": {
                  "get": {
                    "summary": "Retrieve a charge",
                    "responses": {
                      "200": {
                        "description": "Charge details"
                      }
                    }
                  }
                }
              }
            }
            """;

    @Test
    @DisplayName("Parse valid OpenAPI 3.0 document and extract operations")
    void testParseValidOpenApi() {
        OpenApiSpecification spec = validator.parseAndValidate(VALID_OPENAPI_JSON);

        assertThat(spec.getTitle()).isEqualTo("Payment Gateway API");
        assertThat(spec.getVersion()).isEqualTo("1.0.0");
        assertThat(spec.getOpenApiVersion()).isEqualTo("3.0.3");
        assertThat(spec.getServerUrls()).contains("https://api.payments.com/v1");

        assertThat(spec.hasOperation("POST", "/charges")).isTrue();
        assertThat(spec.hasOperation("GET", "/charges/ch_12345")).isTrue();
        assertThat(spec.hasOperation("DELETE", "/charges")).isFalse();

        OpenApiOperation postOp = spec.findOperation("POST", "/charges");
        assertThat(postOp).isNotNull();
        assertThat(postOp.getExpectedStatusCodes()).contains(201, 400);
        assertThat(postOp.getResponseContentTypes()).contains("application/json");
    }

    @Test
    @DisplayName("Parse valid OpenAPI YAML document")
    void testParseValidYaml() {
        String yaml = """
                openapi: 3.0.0
                info:
                  title: Users API
                  version: 2.1.0
                paths:
                  /users:
                    get:
                      summary: List users
                      responses:
                        '200':
                          description: Success
                """;

        OpenApiSpecification spec = validator.parseAndValidate(yaml);
        assertThat(spec.getTitle()).isEqualTo("Users API");
        assertThat(spec.hasOperation("GET", "/users")).isTrue();
    }

    @Test
    @DisplayName("Reject null or blank specification")
    void testRejectNullOrBlank() {
        assertThatThrownBy(() -> validator.parseAndValidate(null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> validator.parseAndValidate("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Reject document missing openapi or swagger root property")
    void testRejectMissingVersion() {
        String invalid = "{\"info\": {\"title\": \"No Version\"}, \"paths\": {}}";
        assertThatThrownBy(() -> validator.parseAndValidate(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing required 'openapi' or 'swagger'");
    }

    @Test
    @DisplayName("Reject document missing paths object")
    void testRejectMissingPaths() {
        String invalid = "{\"openapi\": \"3.0.0\", \"info\": {\"title\": \"No Paths\"}}";
        assertThatThrownBy(() -> validator.parseAndValidate(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing required 'paths'");
    }
}
