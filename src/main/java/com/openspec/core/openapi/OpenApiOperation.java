package com.openspec.core.openapi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenApiOperation {
    private String method;
    private String path;
    private String operationId;
    private String summary;
    private boolean requestBodyRequired;
    private Set<String> parameters;
    private List<Integer> expectedStatusCodes;
    private Set<String> responseContentTypes;
    private boolean deprecated;
}
