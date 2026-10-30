package com.ventureai.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Free-tier web search client used by VentureAI for automatic competitor discovery.
 *
 * Tavily performs the live web search. Gemini is NOT required for this step.
 * This keeps competitor discovery independent from Gemini Search grounding.
 */
@Component
@Slf4j
public class TavilyClient {

    private static final String SEARCH_URL = "https://api.tavily.com/search";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;

    public TavilyClient(
            RestTemplate restTemplate,
            @Value("${TAVILY_API_KEY:}") String apiKey
    ) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Executes one basic Tavily search.
     * Basic search uses one Tavily API credit and returns source URLs and snippets.
     */
    public JsonNode search(String query, int maxResults) {
        if (!isEnabled()) {
            throw new IllegalStateException(
                    "Tavily is not configured. Set TAVILY_API_KEY in the backend environment."
            );
        }

        Map<String, Object> body = Map.of(
                "query", query,
                "search_depth", "basic",
                "chunks_per_source", 2,
                "max_results", Math.max(1, Math.min(maxResults, 10)),
                "topic", "general",
                "include_answer", false,
                "include_raw_content", false,
                "include_images", false,
                "include_favicon", false,
                "safe_search", true
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

        try {
            String response = restTemplate.postForObject(
                    SEARCH_URL,
                    request,
                    String.class
            );

            if (response == null || response.isBlank()) {
                throw new IllegalStateException("Tavily returned an empty response.");
            }

            return objectMapper.readTree(response);

        } catch (RestClientException ex) {
            log.error("Tavily search failed: {}", ex.getMessage());
            throw new IllegalStateException("Tavily search failed.", ex);
        } catch (Exception ex) {
            log.error("Unable to parse Tavily response: {}", ex.getMessage());
            throw new IllegalStateException("Invalid Tavily response.", ex);
        }
    }
}
