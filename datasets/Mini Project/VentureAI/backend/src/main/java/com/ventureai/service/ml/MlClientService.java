package com.ventureai.service.ml;

import com.ventureai.config.MlServiceProperties;
import com.ventureai.entity.Startup;
import com.ventureai.exception.AiServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MlClientService {

    private final RestTemplate restTemplate;
    private final MlServiceProperties mlServiceProperties;

    /**
     * Extract business/NLP features from the Python ML service.
     *
     * Flow:
     * React -> Spring Boot -> Python ML Service
     */
    public NlpExtractionResult extractNlpFeatures(Startup startup) {

        Map<String, Object> payload = new HashMap<>();

        payload.put(
                "startup_name",
                startup.getName()
        );

        payload.put(
                "description",
                nullToEmpty(startup.getDescription())
        );

        payload.put(
                "problem",
                nullToEmpty(startup.getProblem())
        );

        payload.put(
                "solution",
                nullToEmpty(startup.getSolution())
        );

        payload.put(
                "target_market",
                nullToEmpty(startup.getTargetMarket())
        );

        payload.put(
                "business_model",
                nullToEmpty(startup.getBusinessModel())
        );

        payload.put(
                "revenue_model",
                nullToEmpty(startup.getRevenueModel())
        );

        payload.put(
                "unique_value_proposition",
                nullToEmpty(
                        startup.getUniqueValueProposition()
                )
        );

        try {

            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(
                    MediaType.APPLICATION_JSON
            );

            HttpEntity<Map<String, Object>> request =
                    new HttpEntity<>(
                            payload,
                            headers
                    );

            Map response =
                    restTemplate.postForObject(
                            mlServiceProperties.getBaseUrl()
                                    + "/nlp/extract",
                            request,
                            Map.class
                    );

            if (response == null) {
                throw new AiServiceException(
                        "Empty response from BART ML service /nlp/extract"
                );
            }

            return mapToNlpResult(response);

        } catch (RestClientException ex) {

            log.error(
                    "BART ML service /nlp/extract unavailable: {}",
                    ex.getMessage()
            );

            return localNlpFallback(startup);
        }
    }

    /**
     * Send structured startup features to the Python BART ML service.
     *
     * Flow:
     *
     * React
     *   -> Spring Boot
     *   -> FastAPI
     *   -> BART
     *   -> FastAPI
     *   -> Spring Boot
     *   -> React
     *
     * BART returns:
     * - viability score
     * - classification
     * - confidence
     */
    public MlPredictionResult predictViability(
            Map<String, Object> features
    ) {

        try {

            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(
                    MediaType.APPLICATION_JSON
            );

            HttpEntity<Map<String, Object>> request =
                    new HttpEntity<>(
                            features,
                            headers
                    );

            log.info(
                    "Sending startup prediction request to BART ML service: {}",
                    mlServiceProperties.getBaseUrl()
            );

            Map response =
                    restTemplate.postForObject(
                            mlServiceProperties.getBaseUrl()
                                    + "/predict",
                            request,
                            Map.class
                    );

            if (response == null) {

                throw new AiServiceException(
                        "Empty response from BART ML service"
                );
            }

            Object scoreValue =
                    response.get("score");

            Object classificationValue =
                    response.get("classification");

            Object confidenceValue =
                    response.get("confidence");

            if (!(scoreValue instanceof Number)
                    || classificationValue == null
                    || !(confidenceValue instanceof Number)) {

                throw new AiServiceException(
                        "Invalid prediction response from BART ML service"
                );
            }

            double score =
                    ((Number) scoreValue).doubleValue();

            String classification =
                    classificationValue.toString();

            double confidence =
                    ((Number) confidenceValue).doubleValue();

            log.info(
                    "BART prediction successful: score={}, classification={}, confidence={}",
                    score,
                    classification,
                    confidence
            );

            return new MlPredictionResult(
                    score,
                    classification,
                    confidence
            );

        } catch (RestClientException ex) {

            log.error(
                    "BART ML service /predict unavailable: {}",
                    ex.getMessage()
            );

            /*
             * Do NOT generate a fake/local prediction.
             *
             * BART is the only active viability model.
             */
            throw new AiServiceException(
                    "BART ML service is unavailable. "
                            + "Please make sure the Python ML service "
                            + "is running on http://127.0.0.1:8001"
            );
        }
    }

    // ============================================================
    // LOCAL NLP FALLBACK
    // ============================================================

    /**
     * Local NLP fallback only.
     *
     * This is NOT a prediction model.
     * Viability prediction always comes from BART.
     */
    private NlpExtractionResult localNlpFallback(
            Startup startup
    ) {

        String text = String.join(
                " ",
                nullToEmpty(startup.getDescription()),
                nullToEmpty(startup.getProblem()),
                nullToEmpty(startup.getSolution()),
                nullToEmpty(startup.getTargetMarket())
        ).trim();

        Map<String, Double> features =
                new HashMap<>();

        features.put(
                "problem_severity",
                55.0
        );

        features.put(
                "unique_value_prop_score",
                55.0
        );

        features.put(
                "scalability_signal",
                50.0
        );

        features.put(
                "business_model_clarity",
                50.0
        );

        String[] tokens =
                text.isBlank()
                        ? new String[0]
                        : text.split("\\s+");

        return new NlpExtractionResult(
                startup.getName(),

                startup.getIndustry() != null
                        ? startup.getIndustry()
                        : "Other",

                java.util.List.of(
                        text.isBlank()
                                ? "startup"
                                : tokens[0]
                ),

                truncate(startup.getProblem()),

                truncate(startup.getTargetMarket()),

                truncate(startup.getSolution()),

                tokens.length,

                features
        );
    }

    // ============================================================
    // RESPONSE MAPPING
    // ============================================================

    @SuppressWarnings("unchecked")
    private NlpExtractionResult mapToNlpResult(
            Map response
    ) {

        if (response == null) {

            throw new AiServiceException(
                    "Empty response from ML /nlp/extract"
            );
        }

        Map<String, Object> featuresRaw =
                (Map<String, Object>)
                        response.get("features");

        Map<String, Double> features =
                new HashMap<>();

        if (featuresRaw != null) {

            featuresRaw.forEach(
                    (key, value) -> {

                        if (value instanceof Number number) {

                            features.put(
                                    key,
                                    number.doubleValue()
                            );
                        }
                    }
            );
        }

        java.util.List<String> keywords =
                response.get("keywords")
                        instanceof java.util.List
                        ? (java.util.List<String>)
                                response.get("keywords")
                        : java.util.List.of();

        return new NlpExtractionResult(
                (String) response.get("startup_name"),

                (String) response.get("industry"),

                keywords,

                (String) response.get("problem_summary"),

                (String) response.get(
                        "target_customer_summary"
                ),

                (String) response.get(
                        "solution_summary"
                ),

                response.get("tokens_processed")
                        instanceof Number number
                        ? number.intValue()
                        : 0,

                features
        );
    }

    // ============================================================
    // UTILITY METHODS
    // ============================================================

    private String truncate(String value) {

        if (value == null) {
            return "";
        }

        return value.length() > 280
                ? value.substring(0, 280)
                : value;
    }

    private String nullToEmpty(String value) {

        return value == null
                ? ""
                : value;
    }
}