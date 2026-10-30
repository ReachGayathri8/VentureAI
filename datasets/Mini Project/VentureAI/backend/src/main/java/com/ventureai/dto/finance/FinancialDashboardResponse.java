package com.ventureai.dto.finance;

import java.math.BigDecimal;
import java.util.List;

public record FinancialDashboardResponse(
        FinancialSummaryDto summary,
        BreakEvenDto breakEven,

        /**
         * Balance sheet as of the Year 1 snapshot (Month 12 of the
         * projection, or the last available month if the projection is
         * shorter than a year). This is NOT the end of the full
         * projection period - see maxProjectedFinancingRequirement below
         * for that figure.
         */
        BalanceSheetDto balanceSheet,

        List<PnLRowDto> pnlMonthly,
        List<CashFlowRowDto> cashFlowMonthly,
        List<RevenueProjectionYearDto> revenueProjectionYearly,

        /**
         * The largest additional financing the projection implies would be
         * needed at any point across the FULL projection period (which can
         * be later than Year 1, e.g. Month 36 of a 3-year plan). Distinct
         * from balanceSheet's Year-1-only projectedFundingGap.
         */
        BigDecimal maxProjectedFinancingRequirement
) {
}