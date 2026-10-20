package com.openspec.core.protocol.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.openspec.core.protocol.enums.FindingSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestFindingDto {
    private String id;
    private FindingSeverity severity;
    private String title;
    private String description;
    private String relatedCommandId;
    private String evidence;
}
