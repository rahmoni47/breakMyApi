package com.openspec.core.protocol.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestCommandDto {

    @NotBlank(message = "commandId is required")
    private String commandId;

    private int order;

    @NotBlank(message = "command name is required")
    private String name;

    private String description;

    private String purpose;

    @NotNull(message = "request details are required")
    private TestCommandRequestDto request;

    private TestCommandExpectedDto expected;

    private Long timeoutMs;
}
