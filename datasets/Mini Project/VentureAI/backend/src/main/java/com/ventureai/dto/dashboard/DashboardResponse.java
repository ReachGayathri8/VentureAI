package com.ventureai.dto.dashboard;

import com.ventureai.dto.validation.ValidationHistoryItemDto;

import java.util.List;
import java.util.Map;

public record DashboardResponse(
        long totalValidations,
        double averageViabilityScore,
        long highPotentialCount,
        long reportsGenerated,
        List<ValidationHistoryItemDto> recentValidations,
        List<ViabilityHistoryPointDto> viabilityScoreHistory,
        Map<String, Long> industryDistribution,
        List<ViabilityHistoryPointDto> marketOpportunityTrend,
        List<ViabilityHistoryPointDto> financialProjectionTrend
) {
}
