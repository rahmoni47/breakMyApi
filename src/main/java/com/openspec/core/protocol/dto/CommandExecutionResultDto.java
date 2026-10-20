package com.openspec.core.protocol.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommandExecutionResultDto {
    private String commandId;
    private int order;
    private String name;
    private ActualRequestDto actualRequest;
    private Integer statusCode;
    private Map<String, String> responseHeaders;
    private String responseBody;
    private long durationMs;
    private String networkError;
    private CommandValidationEvaluationDto backendValidation;
}
