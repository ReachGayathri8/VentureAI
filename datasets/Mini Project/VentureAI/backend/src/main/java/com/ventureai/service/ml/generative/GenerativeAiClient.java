package com.ventureai.service.ml.generative;

import com.ventureai.config.MlServiceProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class GenerativeAiClient {

    private final RestTemplate restTemplate;
    private final MlServiceProperties mlServiceProperties;

    public String generateQwen(String prompt) {
        return generate("/generate/qwen", prompt);
    }

    public String generateFlanT5(String prompt) {
        return generate("/generate/flant5", prompt);
    }

    private String generate(String endpoint, String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        String url = mlServiceProperties.getBaseUrl()
                + endpoint
                + "?prompt="
                + java.net.URLEncoder.encode(
                        prompt,
                        java.nio.charset.StandardCharsets.UTF_8
                );

        Map response = restTemplate.postForObject(
                url,
                request,
                Map.class
        );

        if (response == null || response.get("response") == null) {
            throw new RuntimeException(
                    "Empty response from ML generative service"
            );
        }

        return response.get("response").toString();
    }
}