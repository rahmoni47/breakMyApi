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
public class TestCommandRequestDto {
    private String method;
    private String path;
    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private Object body;
}
