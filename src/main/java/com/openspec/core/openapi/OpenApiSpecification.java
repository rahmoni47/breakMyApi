package com.openspec.core.openapi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenApiSpecification {
    private String title;
    private String version;
    private String openApiVersion;
    private List<String> serverUrls;
    private Map<String, OpenApiOperation> operations; // Key: "METHOD /path"

    public boolean hasOperation(String method, String path) {
        if (operations == null || method == null || path == null) {
            return false;
        }
        String directKey = method.toUpperCase() + " " + path;
        if (operations.containsKey(directKey)) {
            return true;
        }
        // Check template matching e.g. /users/{id} against /users/123
        return operations.entrySet().stream().anyMatch(entry -> {
            String[] parts = entry.getKey().split(" ", 2);
            if (parts.length == 2 && parts[0].equalsIgnoreCase(method)) {
                return matchesPath(parts[1], path);
            }
            return false;
        });
    }

    public OpenApiOperation findOperation(String method, String path) {
        if (operations == null || method == null || path == null) {
            return null;
        }
        String directKey = method.toUpperCase() + " " + path;
        OpenApiOperation direct = operations.get(directKey);
        if (direct != null) {
            return direct;
        }
        return operations.entrySet().stream()
                .filter(entry -> {
                    String[] parts = entry.getKey().split(" ", 2);
                    return parts.length == 2 && parts[0].equalsIgnoreCase(method) && matchesPath(parts[1], path);
                })
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private boolean matchesPath(String template, String actual) {
        // Replace {param} with regex [^/]+
        String regex = template.replaceAll("\\{[^/]+}", "[^/]+");
        return actual.matches(regex);
    }
}
