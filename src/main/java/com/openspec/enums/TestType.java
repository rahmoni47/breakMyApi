package com.openspec.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Execution profile and depth of API tests")
public enum TestType {
    @Schema(description = "Fast sanity and schema conformance checks")
    FAST,

    @Schema(description = "Standard testing including boundary values and common edge cases")
    MEDIUM,

    @Schema(description = "Deep fuzzing, stateful flow exploration, and vulnerability probes")
    DEEP
}
