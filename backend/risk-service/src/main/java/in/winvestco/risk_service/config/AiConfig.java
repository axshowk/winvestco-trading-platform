package in.winvestco.risk_service.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    private final GeminiProperties geminiProperties;

    public AiConfig(GeminiProperties geminiProperties) {
        this.geminiProperties = geminiProperties;
    }

    @Bean
    public ChatLanguageModel geminiChatModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(geminiProperties.getApiKey())
                .modelName(geminiProperties.getModelName())
                .temperature(geminiProperties.getTemperature())
                .logRequestsAndResponses(true)
                .build();
    }
}