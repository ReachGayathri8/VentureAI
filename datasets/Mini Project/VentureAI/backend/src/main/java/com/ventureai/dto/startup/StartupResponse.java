package com.ventureai.dto.startup;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record StartupResponse(
        // STEP 1 — Basic Information
        Long id,
        String name,
        String industry,
        String businessCategory,
        String startupIdea,
        String description,
        String problem,
        String solution,
        String targetMarket,
        String targetCustomers,
        String businessModel,
        String revenueModel,
        String uniqueValueProposition,
        String marketingStrategy,
        String location,

        // Founder / startup settings
        Double founderExperience,
        Boolean autoDiscoverCompetitors,

        // STEP 3 — Financial Information
        BigDecimal initialInvestment,
        BigDecimal founderInvestment,
        BigDecimal externalFunding,
        BigDecimal monthlyOperatingCost,
        BigDecimal employeeCost,
        BigDecimal marketingCost,
        BigDecimal infrastructureCost,
        BigDecimal productDevelopmentCost,
        Integer expectedMonthlyCustomers,
        BigDecimal averageCustomerRevenue,
        Double expectedGrowthRate,
        Integer projectionPeriodYears,
        BigDecimal fixedCosts,
        BigDecimal variableCostPerUnit,
        BigDecimal sellingPricePerUnit,

        // Metadata
        LocalDateTime createdAt,

        // Competitors
        List<CompetitorDto> competitors,

        // Latest validation
        Double latestViabilityScore,
        String latestClassification,
        String latestRiskLevel
) {
}