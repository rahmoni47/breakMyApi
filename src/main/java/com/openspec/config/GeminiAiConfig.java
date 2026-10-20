package com.openspec.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring configuration providing Google AI Studio Gemini integration via
 * Spring AI's OpenAI-compatible ChatClient abstraction.
 */
@Configuration
public class GeminiAiConfig {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiConfig.class);

    @Value("${gemini.api.key:${GEMINI_API_KEY:}}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-2.5-flash-lite}")
    private String modelName;

    @Value("${gemini.api.base-url:https://generativelanguage.googleapis.com/v1beta/openai}")
    private String baseUrl;

    @Bean
    @Primary
    @ConditionalOnProperty(name = "gemini.api.key")
    public ChatModel geminiChatModel() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini API key is not configured. Live AI calls will not be available.");
            return null;
        }

        log.info("Configuring Google AI Studio Gemini ChatModel (baseUrl: {}, model: {})", baseUrl, modelName);
        OpenAiApi openAiApi = new OpenAiApi(baseUrl, apiKey);
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel(modelName)
                .withTemperature(0.2)
                .build();

        return new OpenAiChatModel(openAiApi, options);
    }

    @Bean
    @Primary
    public ChatClient chatClient(ChatModel chatModel) {
        if (chatModel == null) {
            log.warn("No ChatModel bean available; ChatClient will remain null.");
            return null;
        }
        return ChatClient.builder(chatModel).build();
    }
}
