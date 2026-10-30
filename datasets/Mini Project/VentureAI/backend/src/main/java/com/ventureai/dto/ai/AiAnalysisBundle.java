package com.ventureai.dto.ai;

/**
 * Everything the AI Business Intelligence Engine produces for one
 * validation run, bundled together so ValidationService can persist it in
 * a single pass. Produced either by GeminiAiService (live Gemini call) or
 * its rule-based fallback generator — the rest of the system does not care
 * which produced it.
 */
public record AiAnalysisBundle(
        SwotDto swot,
        String competitiveAdvantage,
        String marketGap,
        String differentiationStrategy,
        MarketOpportunityResponse marketOpportunity,
        RecommendationsResponse recommendations,
        RiskAnalysisResponse riskAnalysis,
        String finalVerdict,
        boolean liveAiGenerated
) {
}
