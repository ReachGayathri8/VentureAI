package com.ventureai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.gemini")
public class GeminiProperties {
    private String apiKey;
    private String model;
    private String baseUrl;
    private boolean enabled;
}
