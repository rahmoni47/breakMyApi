package com.openspec.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Supported open-source AI models. Users must choose strictly from this predefined enum list.")
public enum OpenSourceAiModel {
    @Schema(description = "Meta Llama 3.3 70B Instruct - High-accuracy generalist open weights model")
    LLAMA_3_3_70B,

    @Schema(description = "Meta Llama 3.1 8B Instruct - Ultra-fast, lightweight model ideal for quick sanity checks")
    LLAMA_3_1_8B,

    @Schema(description = "Meta Llama 3.1 70B Instruct - Robust enterprise open model")
    LLAMA_3_1_70B,

    @Schema(description = "DeepSeek R1 - Advanced open-weights reasoning model for complex test flow derivation")
    DEEPSEEK_R1,

    @Schema(description = "DeepSeek V3 - High-parameter Mixture-of-Experts (MoE) general language model")
    DEEPSEEK_V3,

    @Schema(description = "Mistral 7B Instruct - Efficient and accurate open model")
    MISTRAL_7B,

    @Schema(description = "Mixtral 8x7B MoE Instruct - High-capacity sparse mixture-of-experts model")
    MIXTRAL_8X7B,

    @Schema(description = "Qwen 2.5 72B Instruct - Comprehensive multilingual and API reasoning model")
    QWEN_2_5_72B,

    @Schema(description = "Meta CodeLlama 70B Instruct - Specialized code synthesis and API test generator")
    CODELLAMA_70B,

    @Schema(description = "Microsoft Phi-4 14B - State-of-the-art small language reasoning model")
    PHI_4
}
