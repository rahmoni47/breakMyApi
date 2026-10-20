package com.openspec.core.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openspec.core.context.TestContext;
import com.openspec.core.protocol.dto.ActualRequestDto;
import com.openspec.core.protocol.dto.CommandExecutionResultDto;
import com.openspec.core.protocol.dto.CommandValidationEvaluationDto;
import com.openspec.core.protocol.dto.TestCommandDto;
import com.openspec.core.protocol.dto.TestCommandRequestDto;
import com.openspec.core.validator.TestResultValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of {@link TestCommandExecutor} executing HTTP requests safely.
 */
@Component
@RequiredArgsConstructor
public class DefaultTestCommandExecutor implements TestCommandExecutor {

    private final TestResultValidator testResultValidator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_RESPONSE_BODY_LENGTH = 16 * 1024; // 16 KB retention
    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization", "api-key", "x-api-key", "cookie", "set-cookie", "token", "secret", "proxy-authorization"
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public CommandExecutionResultDto execute(TestCommandDto command, URI resolvedUri, TestContext context) {
        TestCommandRequestDto req = command.getRequest();
        long timeoutMs = command.getTimeoutMs() != null && command.getTimeoutMs() > 0 ?
                command.getTimeoutMs() : context.getDefaultTimeoutMs();

        // Build full URL including query parameters
        URI targetUri = buildUriWithQueryParams(resolvedUri, req.getQueryParams());

        // Prepare request body
        String bodyString = serializeBody(req.getBody());
        HttpRequest.BodyPublisher bodyPublisher = bodyString != null ?
                HttpRequest.BodyPublishers.ofString(bodyString) :
                HttpRequest.BodyPublishers.noBody();

        // Build HttpRequest
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(targetUri)
                .timeout(Duration.ofMillis(timeoutMs));

        // Add method & body
        String method = req.getMethod().toUpperCase();
        switch (method) {
            case "GET" -> reqBuilder.GET();
            case "DELETE" -> reqBuilder.DELETE();
            case "POST" -> reqBuilder.POST(bodyPublisher);
            case "PUT" -> reqBuilder.PUT(bodyPublisher);
            default -> reqBuilder.method(method, bodyPublisher);
        }

        // Add headers
        Map<String, String> safeHeaders = new HashMap<>();
        if (req.getHeaders() != null) {
            req.getHeaders().forEach((k, v) -> {
                if (k != null && v != null) {
                    try {
                        reqBuilder.header(k, v);
                    } catch (IllegalArgumentException ignored) {
                        // Restricted headers in Java HttpClient (e.g. Host, Content-Length)
                    }
                    safeHeaders.put(k, maskIfSensitive(k, v));
                }
            });
        }

        // Recorded actual request representation
        ActualRequestDto actualRequest = ActualRequestDto.builder()
                .resolvedUrl(targetUri.toString())
                .method(method)
                .headers(safeHeaders)
                .body(bodyString != null ? truncate(bodyString, 1024) : null)
                .build();

        long startTime = System.currentTimeMillis();
        Integer statusCode = null;
        Map<String, String> responseHeaders = new HashMap<>();
        String responseBody = null;
        String networkError = null;

        try {
            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            statusCode = response.statusCode();

            // Extract response headers (masking any sensitive headers like Set-Cookie)
            response.headers().map().forEach((k, vList) -> {
                String val = String.join(", ", vList);
                responseHeaders.put(k, maskIfSensitive(k, val));
            });

            responseBody = truncate(response.body(), MAX_RESPONSE_BODY_LENGTH);

        } catch (HttpTimeoutException ex) {
            networkError = "Request timed out after " + timeoutMs + "ms";
        } catch (Exception ex) {
            networkError = "Network error: " + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        }

        long durationMs = System.currentTimeMillis() - startTime;

        // Perform backend evaluation
        CommandValidationEvaluationDto evaluation = testResultValidator.evaluate(
                command, statusCode, responseHeaders, networkError
        );

        return CommandExecutionResultDto.builder()
                .commandId(command.getCommandId())
                .order(command.getOrder())
                .name(command.getName())
                .actualRequest(actualRequest)
                .statusCode(statusCode)
                .responseHeaders(responseHeaders)
                .responseBody(responseBody)
                .durationMs(durationMs)
                .networkError(networkError)
                .backendValidation(evaluation)
                .build();
    }

    private URI buildUriWithQueryParams(URI baseUri, Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return baseUri;
        }
        StringBuilder queryBuilder = new StringBuilder();
        if (baseUri.getQuery() != null && !baseUri.getQuery().isBlank()) {
            queryBuilder.append(baseUri.getQuery()).append("&");
        }
        queryParams.forEach((k, v) -> {
            if (k != null && v != null) {
                queryBuilder.append(k).append("=").append(v).append("&");
            }
        });
        if (queryBuilder.length() > 0 && queryBuilder.charAt(queryBuilder.length() - 1) == '&') {
            queryBuilder.deleteCharAt(queryBuilder.length() - 1);
        }

        try {
            return new URI(
                    baseUri.getScheme(),
                    baseUri.getAuthority(),
                    baseUri.getPath(),
                    queryBuilder.toString(),
                    baseUri.getFragment()
            );
        } catch (Exception e) {
            return baseUri;
        }
    }

    private String serializeBody(Object body) {
        if (body == null) {
            return null;
        }
        if (body instanceof String str) {
            return str;
        }
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            return String.valueOf(body);
        }
    }

    private String maskIfSensitive(String headerName, String value) {
        if (headerName != null && SENSITIVE_HEADERS.contains(headerName.toLowerCase())) {
            return "****** (REDACTED)";
        }
        return value;
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "... [truncated]";
    }
}
