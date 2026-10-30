package com.ventureai.dto.ai;

import com.ventureai.dto.startup.CompetitorDto;

import java.util.List;

public record CompetitorAnalysisResponse(
        List<CompetitorDto> competitors,
        int yourPriceRating,
        int yourFeatureRating,
        int yourMarketReachRating,
        int yourInnovationRating,
        String competitiveAdvantage,
        String marketGap,
        String recommendedDifferentiationStrategy
) {
}
