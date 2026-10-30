package com.ventureai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.ml-service")
public class MlServiceProperties {
    private String baseUrl;
}
