package com.ventureai.service.ml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NlpExtractionResult(
        String startupName,
        String industry,
        List<String> keywords,
        String problemSummary,
        String targetCustomerSummary,
        String solutionSummary,
        int tokensProcessed,
        Map<String, Double> features
) {
    public double feature(String name, double fallback) {
        if (features == null || !features.containsKey(name)) return fallback;
        return features.get(name);
    }
}
