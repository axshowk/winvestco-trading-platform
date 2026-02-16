package in.winvestco.risk_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "langchain4j.google-ai-gemini.chat-model")
public class GeminiProperties {
    private String apiKey;
    private String modelName;
    private Double temperature;
}
