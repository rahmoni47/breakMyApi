package com.openspec.core.protocol.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.openspec.core.protocol.enums.CommandStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommandAnalysisResultDto {
    private String commandId;
    private CommandStatus status;
    private String explanation;
    private String evidence;
}
