package com.ventureai.dto.ai;

import java.util.List;

public record RiskAnalysisResponse(
        List<RiskCategoryDto> categories,
        double overallRiskScore,
        String overallRiskLevel
) {
}
