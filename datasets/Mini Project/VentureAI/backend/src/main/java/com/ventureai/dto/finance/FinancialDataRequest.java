package com.ventureai.dto.finance;

import java.math.BigDecimal;

public record FinancialDataRequest(

        // ============================================================
        // FUNDING
        // ============================================================

        BigDecimal initialInvestment,

        BigDecimal founderInvestment,

        BigDecimal externalFunding,

        String financingType,


        // ============================================================
        // OPERATING COSTS
        // ============================================================

        BigDecimal monthlyOperatingCost,

        BigDecimal employeeCost,

        BigDecimal marketingCost,

        BigDecimal infrastructureCost,

        BigDecimal productDevelopmentCost,


        // ============================================================
        // CUSTOMER & REVENUE
        // ============================================================

        Integer expectedMonthlyCustomers,

        BigDecimal averageCustomerRevenue,

        Double expectedGrowthRate,

        Integer projectionPeriodYears,


        // ============================================================
        // UNIT ECONOMICS
        // ============================================================

        BigDecimal fixedCosts,

        BigDecimal variableCostPerUnit,

        BigDecimal sellingPricePerUnit,


        // ============================================================
        // CUSTOMER ACQUISITION
        // ============================================================

        Integer monthlyNewCustomers,

        BigDecimal customerAcquisitionCost,

        Double monthlyChurnRate,


        // ============================================================
        // CUSTOMER VALUE
        // ============================================================

        Double averageCustomerLifetimeMonths,


        // ============================================================
        // TAX
        // ============================================================

        Double taxRate,


        // ============================================================
        // CAPITAL EXPENDITURE
        // ============================================================

        BigDecimal capitalExpenditure,

        Integer assetUsefulLifeMonths,


        // ============================================================
        // DEBT
        // ============================================================

        BigDecimal debtAmount,

        Double annualInterestRate,

        Integer debtTermMonths,


        // ============================================================
        // WORKING CAPITAL
        // ============================================================

        Double accountsReceivableRate,

        Double accountsPayableRate,

        Double inventoryRate,


        // ============================================================
        // CASH MANAGEMENT
        // ============================================================

        BigDecimal minimumCashReserve

) {
}
