package com.ventureai.dto.startup;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public record StartupRequest(
        // STEP 1 — Basic Information
        @NotBlank(message = "Startup name is required") String startupName,
        @NotBlank(message = "Startup idea is required") String startupIdea,
        @NotBlank(message = "Industry is required") String industry,
        String businessCategory,
        String targetMarket,
        String location,
        String businessModel,

        // STEP 2 — Business Details
        String problem,
        String solution,
        String targetCustomers,
        String uniqueValueProposition,
        String revenueModel,
        String marketingStrategy,
        String businessDescription,

        // STEP 3 — Financial Input
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

        // STEP 4 — Competitors
        @Valid List<CompetitorRequest> competitors,
        Boolean autoDiscoverCompetitors,

        // Optional soft signal, section 9 ("founder/business experience if available")
        Double founderExperience
) {
}
