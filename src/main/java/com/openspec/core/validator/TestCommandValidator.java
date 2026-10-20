package com.openspec.core.validator;

import com.openspec.core.context.TestContext;
import com.openspec.core.protocol.dto.TestCommandDto;
import com.openspec.core.protocol.dto.TestCommandRequestDto;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.util.Set;

/**
 * Validates test commands for schema conformance, OpenAPI consistency, and security boundaries.
 */
@Component
public class TestCommandValidator {

    private static final Set<String> ALLOWED_METHODS = Set.of(
            "GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"
    );

    public record CommandValidationResult(boolean valid, String reason, URI resolvedUri) {
        public static CommandValidationResult success(URI uri) {
            return new CommandValidationResult(true, null, uri);
        }

        public static CommandValidationResult failure(String reason) {
            return new CommandValidationResult(false, reason, null);
        }
    }

    /**
     * Validates an individual command against the test context, target base URL, and OpenAPI specification.
     */
    public CommandValidationResult validate(TestCommandDto command, TestContext context) {
        if (command == null) {
            return CommandValidationResult.failure("Command cannot be null");
        }
        if (command.getCommandId() == null || command.getCommandId().isBlank()) {
            return CommandValidationResult.failure("Command commandId is required");
        }
        if (command.getName() == null || command.getName().isBlank()) {
            return CommandValidationResult.failure("Command name is required");
        }
        TestCommandRequestDto request = command.getRequest();
        if (request == null) {
            return CommandValidationResult.failure("Command request details are required");
        }
        if (request.getMethod() == null || request.getMethod().isBlank()) {
            return CommandValidationResult.failure("Command HTTP method is required");
        }

        String method = request.getMethod().toUpperCase();
        if (!ALLOWED_METHODS.contains(method)) {
            return CommandValidationResult.failure("Unsupported HTTP method: " + method);
        }

        if (request.getPath() == null || request.getPath().isBlank()) {
            return CommandValidationResult.failure("Command request path is required");
        }

        // Validate consistency with OpenAPI document
        if (context.getParsedSpec() != null) {
            boolean hasOp = context.getParsedSpec().hasOperation(method, request.getPath());
            if (!hasOp) {
                return CommandValidationResult.failure(
                        String.format("Operation %s %s is not documented in the provided OpenAPI specification",
                                method, request.getPath())
                );
            }
        }

        // Resolve URI against targetBaseUrl
        String baseUrl = context.getTargetBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            return CommandValidationResult.failure("Target base URL is not configured for test run");
        }

        URI resolvedUri;
        try {
            URI baseUri = URI.create(baseUrl);
            String cleanBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            String path = request.getPath().startsWith("/") ? request.getPath() : "/" + request.getPath();
            resolvedUri = URI.create(cleanBase + path);

            // Ensure the host was not altered/escaped
            if (!baseUri.getHost().equalsIgnoreCase(resolvedUri.getHost())) {
                return CommandValidationResult.failure("Resolved host (" + resolvedUri.getHost() +
                        ") does not match target host (" + baseUri.getHost() + ")");
            }

            // Security guard: SSRF / Private network target prevention
            if (!context.isAllowLocalNetwork()) {
                String host = resolvedUri.getHost();
                if (isProhibitedDestination(host)) {
                    return CommandValidationResult.failure("Target destination host is prohibited by security policy: " + host);
                }
            }

        } catch (Exception ex) {
            return CommandValidationResult.failure("Failed to resolve valid target URI: " + ex.getMessage());
        }

        return CommandValidationResult.success(resolvedUri);
    }

    private boolean isProhibitedDestination(String host) {
        if (host == null) {
            return true;
        }
        String lower = host.toLowerCase().trim();
        if (lower.equals("localhost") || lower.equals("127.0.0.1") || lower.equals("::1") || lower.equals("169.254.169.254")) {
            return true;
        }

        // Check for private subnet IP resolution
        try {
            InetAddress address = InetAddress.getByName(host);
            if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()) {
                return true;
            }
        } catch (Exception ignored) {
            // DNS resolution failure will be handled by HTTP transport
        }
        return false;
    }
}
