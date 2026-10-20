package com.openspec.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Supported open-source AI model inference providers")
public enum AiProvider {
    @Schema(description = "Groq - Ultra-low latency open-source model inference (Llama, Mistral, etc.)")
    GROQ,

    @Schema(description = "Together AI - Fast serverless inference for open-source AI models")
    TOGETHER_AI,

    @Schema(description = "Hugging Face - Serverless and Dedicated Inference Endpoints")
    HUGGING_FACE,

    @Schema(description = "Mistral AI - Official API for open Mistral and Mixtral models")
    MISTRAL_AI,

    @Schema(description = "DeepInfra - Scalable serverless open-source model hosting")
    DEEPINFRA,

    @Schema(description = "Fireworks AI - High-throughput inference for open-weights models")
    FIREWORKS_AI,

    @Schema(description = "OpenRouter - Unified API gateway accessing multiple open-source models")
    OPENROUTER,

    @Schema(description = "Ollama - Local or self-hosted open-source model instance")
    OLLAMA
}
