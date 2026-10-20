package com.openspec.core.validator;

import com.openspec.core.context.TestContext;
import com.openspec.core.openapi.OpenApiOperation;
import com.openspec.core.openapi.OpenApiSpecification;
import com.openspec.core.protocol.dto.TestCommandDto;
import com.openspec.core.protocol.dto.TestCommandRequestDto;
import com.openspec.enums.TestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TestCommandValidatorTests {

    private final TestCommandValidator validator = new TestCommandValidator();
    private TestContext context;

    @BeforeEach
    void setUp() {
        OpenApiSpecification spec = OpenApiSpecification.builder()
                .title("Test API")
                .version("1.0.0")
                .operations(Map.of(
                        "GET /v1/users", OpenApiOperation.builder().method("GET").path("/v1/users").build(),
                        "POST /v1/users", OpenApiOperation.builder().method("POST").path("/v1/users").build()
                ))
                .build();

        context = TestContext.builder()
                .testRunId(UUID.randomUUID())
                .testId("FAST-01")
                .testName("User Service Suite")
                .testType(TestType.FAST)
                .targetBaseUrl("https://api.example.com")
                .openApiDocument("{}")
                .parsedSpec(spec)
                .allowLocalNetwork(false)
                .build();
    }

    @Test
    @DisplayName("Valid command consistent with OpenAPI passes validation")
    void testValidCommandPasses() {
        TestCommandDto command = TestCommandDto.builder()
                .commandId("CMD-01")
                .name("List Users")
                .request(TestCommandRequestDto.builder()
                        .method("GET")
                        .path("/v1/users")
                        .build())
                .build();

        TestCommandValidator.CommandValidationResult result = validator.validate(command, context);
        assertThat(result.valid()).isTrue();
        assertThat(result.resolvedUri().toString()).isEqualTo("https://api.example.com/v1/users");
    }

    @Test
    @DisplayName("Reject command targeting undocumented OpenAPI endpoint")
    void testRejectUndocumentedEndpoint() {
        TestCommandDto command = TestCommandDto.builder()
                .commandId("CMD-02")
                .name("Delete Database")
                .request(TestCommandRequestDto.builder()
                        .method("DELETE")
                        .path("/admin/drop-database")
                        .build())
                .build();

        TestCommandValidator.CommandValidationResult result = validator.validate(command, context);
        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).contains("is not documented in the provided OpenAPI specification");
    }

    @Test
    @DisplayName("Reject command with unsupported HTTP method")
    void testRejectUnsupportedMethod() {
        TestCommandDto command = TestCommandDto.builder()
                .commandId("CMD-03")
                .name("Trace Call")
                .request(TestCommandRequestDto.builder()
                        .method("TRACE")
                        .path("/v1/users")
                        .build())
                .build();

        TestCommandValidator.CommandValidationResult result = validator.validate(command, context);
        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).contains("Unsupported HTTP method");
    }

    @Test
    @DisplayName("Reject attempt to contact prohibited destination (localhost / SSRF)")
    void testRejectProhibitedDestination() {
        TestContext localContext = TestContext.builder()
                .testRunId(UUID.randomUUID())
                .testType(TestType.FAST)
                .targetBaseUrl("http://localhost:8080")
                .openApiDocument("{}")
                .allowLocalNetwork(false) // SSRF guard enabled
                .build();

        TestCommandDto command = TestCommandDto.builder()
                .commandId("CMD-04")
                .name("Local test")
                .request(TestCommandRequestDto.builder()
                        .method("GET")
                        .path("/status")
                        .build())
                .build();

        TestCommandValidator.CommandValidationResult result = validator.validate(command, localContext);
        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).contains("prohibited by security policy");
    }
}
