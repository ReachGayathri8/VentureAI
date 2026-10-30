package com.ventureai.service.ml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MlPredictionResult(
        double score,
        String classification,
        double confidence
) {
}