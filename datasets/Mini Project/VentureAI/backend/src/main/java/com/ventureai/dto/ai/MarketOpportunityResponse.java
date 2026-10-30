package com.ventureai.dto.ai;

import java.util.List;

public record MarketOpportunityResponse(
        String targetMarket,
        String marketDemand,
        String growthPotential,
        List<String> customerSegments,
        List<String> marketTrends,
        List<String> opportunityAreas,
        List<String> majorRisks,
        double marketGrowthScore,
        double demandScore,
        double competitionScore,
        boolean aiEstimated
) {
}
