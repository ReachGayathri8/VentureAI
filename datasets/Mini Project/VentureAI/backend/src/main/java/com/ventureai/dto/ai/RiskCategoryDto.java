package com.ventureai.dto.ai;

public record RiskCategoryDto(
        String category,
        String riskLevel,
        String impact,
        String mitigationStrategy
) {
}
