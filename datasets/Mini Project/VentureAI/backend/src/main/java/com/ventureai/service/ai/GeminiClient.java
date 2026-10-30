package com.ventureai.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventureai.config.GeminiProperties;
import com.ventureai.exception.AiServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
@Slf4j
public class GeminiClient {

    private final RestTemplate restTemplate;
    private final GeminiProperties geminiProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isEnabled() {
        return geminiProperties.isEnabled()
                && geminiProperties.getApiKey() != null
                && !geminiProperties.getApiKey().isBlank()
                && geminiProperties.getModel() != null
                && !geminiProperties.getModel().isBlank()
                && geminiProperties.getBaseUrl() != null
                && !geminiProperties.getBaseUrl().isBlank();
    }

    /**
     * Standard Gemini JSON generation.
     *
     * Used for:
     * - SWOT
     * - recommendations
     * - market analysis
     * - qualitative business intelligence
     */
    public JsonNode generateJson(
            String systemInstruction,
            String userPrompt
    ) {
        if (!isEnabled()) {
            throw new AiServiceException(
                    "Gemini is not enabled or required configuration is missing."
            );
        }

        String url =
                geminiProperties.getBaseUrl()
                        + "/models/"
                        + geminiProperties.getModel()
                        + ":generateContent";

        Map<String, Object> body = Map.of(
                "contents",
                List.of(
                        Map.of(
                                "role",
                                "user",
                                "parts",
                                List.of(
                                        Map.of(
                                                "text",
                                                systemInstruction
                                                        + "\n\n"
                                                        + userPrompt
                                        )
                                )
                        )
                ),
                "generationConfig",
                Map.of(
                        "temperature",
                        0.2,
                        "responseMimeType",
                        "application/json"
                )
        );

        return executeRequest(url, body);
    }

    /**
     * Gemini JSON generation with Google Search grounding.
     *
     * Used for automatic competitor discovery.
     */
    public GroundedJsonResponse generateGroundedJson(
            String systemInstruction,
            String userPrompt
    ) {
        if (!isEnabled()) {
            throw new AiServiceException(
                    "Gemini is not enabled or required configuration is missing."
            );
        }

        String url =
                geminiProperties.getBaseUrl()
                        + "/models/"
                        + geminiProperties.getModel()
                        + ":generateContent";

        Map<String, Object> body = Map.of(
                "contents",
                List.of(
                        Map.of(
                                "role",
                                "user",
                                "parts",
                                List.of(
                                        Map.of(
                                                "text",
                                                systemInstruction
                                                        + "\n\n"
                                                        + userPrompt
                                        )
                                )
                        )
                ),
                "tools",
                List.of(
                        Map.of(
                                "google_search",
                                Map.of()
                        )
                ),
                "generationConfig",
                Map.of(
                        "temperature",
                        0.2,
                        "responseMimeType",
                        "application/json"
                )
        );

        return executeGroundedRequest(url, body);
    }

    /**
     * Executes a normal Gemini request and extracts generated JSON.
     */
    private JsonNode executeRequest(
            String url,
            Map<String, Object> body
    ) {
        try {

            Map<String, Object> response =
                    postRequest(url, body);

            String text =
                    extractText(response);

            if (text == null || text.isBlank()) {
                throw new AiServiceException(
                        "Gemini returned an empty response."
                );
            }

            return objectMapper.readTree(
                    cleanJsonText(text)
            );

        } catch (RestClientException | IOException ex) {

            log.error(
                    "Gemini API call failed: {}",
                    ex.getMessage()
            );

            throw new AiServiceException(
                    "Gemini API call failed",
                    ex
            );
        }
    }

    /**
     * Executes a Gemini request using Google Search grounding.
     */
    private GroundedJsonResponse executeGroundedRequest(
            String url,
            Map<String, Object> body
    ) {
        try {

            Map<String, Object> response =
                    postRequest(url, body);

            String text =
                    extractText(response);

            if (text == null || text.isBlank()) {
                throw new AiServiceException(
                        "Gemini returned an empty grounded response."
                );
            }

            JsonNode json =
                    objectMapper.readTree(
                            cleanJsonText(text)
                    );

            JsonNode groundingMetadata =
                    extractGroundingMetadata(response);

            return new GroundedJsonResponse(
                    json,
                    groundingMetadata
            );

        } catch (RestClientException | IOException ex) {

            log.error(
                    "Grounded Gemini API call failed: {}",
                    ex.getMessage()
            );

            throw new AiServiceException(
                    "Grounded Gemini API call failed",
                    ex
            );
        }
    }

    /**
     * Sends HTTP request to Gemini.
     *
     * 429:
     * Do not repeatedly retry quota errors.
     *
     * 500/502/503/504:
     * Retry because these are temporary server-side failures.
     *
     * Network errors:
     * Retry a limited number of times.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> postRequest(
            String url,
            Map<String, Object> body
    ) {

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        headers.set(
                "x-goog-api-key",
                geminiProperties.getApiKey()
        );

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

        final int maxAttempts = 1;

        for (int attempt = 1;
             attempt <= maxAttempts;
             attempt++) {

            try {

                Map<String, Object> response =
                        restTemplate.postForObject(
                                url,
                                request,
                                Map.class
                        );

                if (response == null) {
                    throw new AiServiceException(
                            "Empty Gemini response."
                    );
                }

                if (attempt > 1) {
                    log.info(
                            "Gemini request succeeded after {} attempt(s).",
                            attempt
                    );
                }

                return response;

            } catch (HttpStatusCodeException ex) {

                int status =
                        ex.getStatusCode().value();

                /*
                 * 429 means quota/rate limit.
                 *
                 * Do not retry because repeated requests
                 * will not restore a daily quota.
                 *
                 * GeminiAiService will automatically use
                 * the rule-based fallback.
                 */
                if (status == 429) {

                    log.warn(
                            "Gemini returned HTTP 429. " +
                            "Using rule-based fallback."
                    );

                    throw new AiServiceException(
                            "Gemini quota/rate limit reached.",
                            ex
                    );
                }

                boolean transientServerError =
                        status == 500
                                || status == 502
                                || status == 503
                                || status == 504;

                /*
                 * Non-transient HTTP errors should not
                 * be retried.
                 */
                if (!transientServerError
                        || attempt == maxAttempts) {

                    log.error(
                            "Gemini API request failed with HTTP {} " +
                            "after {} attempt(s): {}",
                            status,
                            attempt,
                            ex.getMessage()
                    );

                    throw new AiServiceException(
                            "Gemini API call failed with HTTP "
                                    + status,
                            ex
                    );
                }

                /*
                 * Exponential backoff:
                 *
                 * attempt 1 -> ~1500-2250 ms
                 * attempt 2 -> ~3000-3750 ms
                 */
                long baseDelayMs =
                        1500L *
                        (1L << (attempt - 1));

                long jitterMs =
                        ThreadLocalRandom.current()
                                .nextLong(
                                        250L,
                                        750L
                                );

                long delayMs =
                        baseDelayMs + jitterMs;

                log.warn(
                        "Gemini returned HTTP {}. " +
                        "Retrying in {} ms " +
                        "(attempt {}/{}).",
                        status,
                        delayMs,
                        attempt + 1,
                        maxAttempts
                );

                try {

                    Thread.sleep(delayMs);

                } catch (InterruptedException interrupted) {

                    Thread.currentThread().interrupt();

                    throw new AiServiceException(
                            "Gemini retry interrupted.",
                            interrupted
                    );
                }

            } catch (RestClientException ex) {

                if (attempt == maxAttempts) {

                    log.error(
                            "Gemini network request failed " +
                            "after {} attempt(s): {}",
                            attempt,
                            ex.getMessage()
                    );

                    throw new AiServiceException(
                            "Gemini network request failed.",
                            ex
                    );
                }

                long delayMs =
                        1000L * attempt
                                + ThreadLocalRandom.current()
                                .nextLong(
                                        250L,
                                        750L
                                );

                log.warn(
                        "Gemini network error. " +
                        "Retrying in {} ms " +
                        "(attempt {}/{}).",
                        delayMs,
                        attempt + 1,
                        maxAttempts
                );

                try {

                    Thread.sleep(delayMs);

                } catch (InterruptedException interrupted) {

                    Thread.currentThread().interrupt();

                    throw new AiServiceException(
                            "Gemini retry interrupted.",
                            interrupted
                    );
                }
            }
        }

        throw new AiServiceException(
                "Gemini API call failed."
        );
    }

    /**
     * Extracts generated text from Gemini response.
     */
    @SuppressWarnings("unchecked")
    private String extractText(
            Map<String, Object> response
    ) {

        Object candidatesObject =
                response.get("candidates");

        if (!(candidatesObject instanceof List<?> candidates)
                || candidates.isEmpty()) {

            log.error(
                    "Gemini response did not contain candidates: {}",
                    response
            );

            throw new AiServiceException(
                    "No candidates in Gemini response."
            );
        }

        Object firstCandidate =
                candidates.get(0);

        if (!(firstCandidate instanceof Map<?, ?> candidate)) {

            throw new AiServiceException(
                    "Invalid Gemini candidate response."
            );
        }

        Object contentObject =
                candidate.get("content");

        if (!(contentObject instanceof Map<?, ?> content)) {

            throw new AiServiceException(
                    "Gemini response did not contain content."
            );
        }

        Object partsObject =
                content.get("parts");

        if (!(partsObject instanceof List<?> parts)
                || parts.isEmpty()) {

            throw new AiServiceException(
                    "Gemini response did not contain content parts."
            );
        }

        Object firstPart =
                parts.get(0);

        if (!(firstPart instanceof Map<?, ?> part)) {

            throw new AiServiceException(
                    "Invalid Gemini content part."
            );
        }

        Object textObject =
                part.get("text");

        if (!(textObject instanceof String text)
                || text.isBlank()) {

            throw new AiServiceException(
                    "Gemini response did not contain generated text."
            );
        }

        return text;
    }

    /**
     * Extracts grounding metadata returned by Gemini.
     */
    private JsonNode extractGroundingMetadata(
            Map<String, Object> response
    ) {

        try {

            JsonNode root =
                    objectMapper.valueToTree(
                            response
                    );

            JsonNode candidates =
                    root.path("candidates");

            if (!candidates.isArray()
                    || candidates.isEmpty()) {

                return objectMapper
                        .createObjectNode();
            }

            JsonNode metadata =
                    candidates
                            .get(0)
                            .path("groundingMetadata");

            if (metadata.isMissingNode()
                    || metadata.isNull()) {

                return objectMapper
                        .createObjectNode();
            }

            return metadata;

        } catch (Exception ex) {

            log.warn(
                    "Unable to parse Gemini grounding metadata: {}",
                    ex.getMessage()
            );

            return objectMapper
                    .createObjectNode();
        }
    }

    /**
     * Removes accidental Markdown JSON fences.
     */
    private String cleanJsonText(
            String text
    ) {

        String cleaned =
                text.trim();

        if (cleaned.startsWith("```json")) {

            cleaned =
                    cleaned.substring(7);

        } else if (cleaned.startsWith("```")) {

            cleaned =
                    cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {

            cleaned =
                    cleaned.substring(
                            0,
                            cleaned.length() - 3
                    );
        }

        return cleaned.trim();
    }

    /**
     * Result returned by grounded Gemini request.
     */
    public record GroundedJsonResponse(
            JsonNode json,
            JsonNode groundingMetadata
    ) {
    }
}
