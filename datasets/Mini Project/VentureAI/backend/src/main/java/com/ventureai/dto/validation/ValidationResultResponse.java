package com.ventureai.dto.validation;

import java.time.LocalDateTime;

public record ValidationResultResponse(
        Long id,
        Long startupId,
        String startupName,
        double viabilityScore,
        String classification,
        double confidencePercent,
        String riskLevel,
        String marketPotentialLabel,
        String financialFeasibilityLabel,
        String scalabilityLabel,
        ScoreBreakdownDto scoreBreakdown,
        String detectedIndustry,
        LocalDateTime createdAt
) {
}
