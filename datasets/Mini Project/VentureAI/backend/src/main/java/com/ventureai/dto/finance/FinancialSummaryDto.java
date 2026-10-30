package com.ventureai.dto.finance;

import java.math.BigDecimal;

public record FinancialSummaryDto(

        BigDecimal monthlyRevenue,

        BigDecimal annualRevenue,

        BigDecimal monthlyOperatingExpenses,

        BigDecimal grossProfitMonth1,

        BigDecimal ebitdaMonth1,

        BigDecimal ebitMonth1,

        BigDecimal profitBeforeTaxMonth1,

        BigDecimal taxMonth1,

        BigDecimal netProfitMonth1,

        double grossMarginPercent,

        double profitMarginPercent,

        double roiPercent,

        BigDecimal burnRate,

        Double runwayMonths,

        BigDecimal closingCash,

        BigDecimal totalDebt,

        double financialHealthScore

) {
}
