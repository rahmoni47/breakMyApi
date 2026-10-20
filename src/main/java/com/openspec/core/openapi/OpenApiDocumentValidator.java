package com.openspec.core.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service for parsing and validating OpenAPI 3.x and Swagger 2.0 specifications.
 */
@Component
public class OpenApiDocumentValidator {

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final ObjectMapper yamlMapper = new YAMLMapper();

    private static final Set<String> SUPPORTED_HTTP_METHODS = Set.of(
            "GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"
    );

    /**
     * Parses and extracts operations from the supplied OpenAPI/Swagger specification string.
     *
     * @param documentContent JSON or YAML specification
     * @return parsed and validated specification model
     * @throws IllegalArgumentException if document is malformed or invalid
     */
    public OpenApiSpecification parseAndValidate(String documentContent) {
        if (documentContent == null || documentContent.trim().isEmpty()) {
            throw new IllegalArgumentException("OpenAPI document content cannot be null or empty");
        }

        JsonNode rootNode = parseTree(documentContent);

        // Validate basic structure
        String openApiVersion = null;
        if (rootNode.has("openapi")) {
            openApiVersion = rootNode.get("openapi").asText();
        } else if (rootNode.has("swagger")) {
            openApiVersion = rootNode.get("swagger").asText();
        } else {
            throw new IllegalArgumentException("Document is missing required 'openapi' or 'swagger' root property");
        }

        if (!rootNode.has("info")) {
            throw new IllegalArgumentException("Document is missing required 'info' object");
        }

        JsonNode infoNode = rootNode.get("info");
        String title = infoNode.has("title") ? infoNode.get("title").asText() : "Unnamed API";
        String version = infoNode.has("version") ? infoNode.get("version").asText() : "1.0.0";

        if (!rootNode.has("paths") || !rootNode.get("paths").isObject()) {
            throw new IllegalArgumentException("Document is missing required 'paths' object or it is not valid");
        }

        // Extract servers / base URLs
        List<String> serverUrls = new ArrayList<>();
        if (rootNode.has("servers") && rootNode.get("servers").isArray()) {
            for (JsonNode server : rootNode.get("servers")) {
                if (server.has("url")) {
                    serverUrls.add(server.get("url").asText());
                }
            }
        } else if (rootNode.has("host")) { // Swagger 2.0
            String scheme = "http";
            if (rootNode.has("schemes") && rootNode.get("schemes").isArray() && rootNode.get("schemes").size() > 0) {
                scheme = rootNode.get("schemes").get(0).asText();
            }
            String basePath = rootNode.has("basePath") ? rootNode.get("basePath").asText() : "";
            serverUrls.add(scheme + "://" + rootNode.get("host").asText() + basePath);
        }

        // Extract operations
        Map<String, OpenApiOperation> operations = new LinkedHashMap<>();
        JsonNode pathsNode = rootNode.get("paths");
        Iterator<Map.Entry<String, JsonNode>> pathsIterator = pathsNode.fields();

        while (pathsIterator.hasNext()) {
            Map.Entry<String, JsonNode> pathEntry = pathsIterator.next();
            String path = pathEntry.getKey();
            JsonNode pathNode = pathEntry.getValue();

            if (!pathNode.isObject()) {
                continue;
            }

            Iterator<Map.Entry<String, JsonNode>> methodsIterator = pathNode.fields();
            while (methodsIterator.hasNext()) {
                Map.Entry<String, JsonNode> methodEntry = methodsIterator.next();
                String method = methodEntry.getKey().toUpperCase();

                if (!SUPPORTED_HTTP_METHODS.contains(method)) {
                    continue;
                }

                JsonNode opNode = methodEntry.getValue();
                OpenApiOperation operation = buildOperation(method, path, opNode);
                operations.put(method + " " + path, operation);
            }
        }

        return OpenApiSpecification.builder()
                .title(title)
                .version(version)
                .openApiVersion(openApiVersion)
                .serverUrls(serverUrls)
                .operations(operations)
                .build();
    }

    private OpenApiOperation buildOperation(String method, String path, JsonNode opNode) {
        String operationId = opNode.has("operationId") ? opNode.get("operationId").asText() : null;
        String summary = opNode.has("summary") ? opNode.get("summary").asText() : "";
        boolean deprecated = opNode.has("deprecated") && opNode.get("deprecated").asBoolean();

        boolean requestBodyRequired = false;
        if (opNode.has("requestBody")) {
            JsonNode requestBody = opNode.get("requestBody");
            requestBodyRequired = requestBody.has("required") && requestBody.get("required").asBoolean();
        }

        Set<String> parameters = new HashSet<>();
        if (opNode.has("parameters") && opNode.get("parameters").isArray()) {
            for (JsonNode param : opNode.get("parameters")) {
                if (param.has("name")) {
                    parameters.add(param.get("name").asText());
                }
            }
        }

        List<Integer> expectedStatusCodes = new ArrayList<>();
        Set<String> responseContentTypes = new HashSet<>();
        if (opNode.has("responses") && opNode.get("responses").isObject()) {
            Iterator<Map.Entry<String, JsonNode>> responsesIter = opNode.get("responses").fields();
            while (responsesIter.hasNext()) {
                Map.Entry<String, JsonNode> respEntry = responsesIter.next();
                String codeStr = respEntry.getKey();
                try {
                    expectedStatusCodes.add(Integer.parseInt(codeStr));
                } catch (NumberFormatException ignored) {
                    // "default" or wildcard responses
                }

                JsonNode respBody = respEntry.getValue();
                if (respBody.has("content") && respBody.get("content").isObject()) {
                    Iterator<String> contentTypes = respBody.get("content").fieldNames();
                    while (contentTypes.hasNext()) {
                        responseContentTypes.add(contentTypes.next());
                    }
                }
            }
        }

        if (expectedStatusCodes.isEmpty()) {
            // Default expected status if none specified
            expectedStatusCodes.add(200);
        }

        return OpenApiOperation.builder()
                .method(method)
                .path(path)
                .operationId(operationId)
                .summary(summary)
                .requestBodyRequired(requestBodyRequired)
                .parameters(parameters)
                .expectedStatusCodes(expectedStatusCodes)
                .responseContentTypes(responseContentTypes)
                .deprecated(deprecated)
                .build();
    }

    private JsonNode parseTree(String content) {
        try {
            // Try JSON first
            return jsonMapper.readTree(content);
        } catch (Exception jsonEx) {
            try {
                // Try YAML
                return yamlMapper.readTree(content);
            } catch (Exception yamlEx) {
                throw new IllegalArgumentException("Failed to parse document as JSON or YAML: " + jsonEx.getMessage());
            }
        }
    }
}
