package com.saeal.MrDaebackService.voiceOrder.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "groq")
@Getter
@Setter
public class GroqConfig {
    private String apiKey;
    private String sttModel = "whisper-large-v3";
    private String llmModel = "llama-3.3-70b-versatile";
    private String baseUrl = "https://api.groq.com/openai/v1";
}
