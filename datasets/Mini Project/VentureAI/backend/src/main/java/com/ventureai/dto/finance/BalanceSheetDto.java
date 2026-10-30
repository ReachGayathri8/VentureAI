package com.ventureai.dto.finance;

import java.math.BigDecimal;

public record BalanceSheetDto(

        BigDecimal cash,

        BigDecimal accountsReceivable,

        BigDecimal inventory,

        BigDecimal netFixedAssets,

        BigDecimal totalAssets,

        /**
         * Combined financing liability (existingDebt + projectedFundingGap),
         * kept for backward compatibility with existing consumers of this
         * field. New code should prefer the two fields below, which keep
         * actual borrowed debt separate from a projected cash-flow gap.
         */
        BigDecimal debt,

        /**
         * Actual debt entered by the founder (loan principal remaining as
         * of the snapshot period). This is NEVER inflated by a projected
         * cash shortfall.
         */
        BigDecimal existingDebt,

        /**
         * Additional funding the projection implies would be needed to keep
         * cash non-negative as of the snapshot period. This is an estimate
         * derived from the financial model, not a loan the founder has
         * actually taken on.
         */
        BigDecimal projectedFundingGap,

        BigDecimal accountsPayable,

        BigDecimal totalLiabilities,

        BigDecimal founderCapital,

        BigDecimal retainedEarnings,

        BigDecimal totalEquity,

        BigDecimal totalLiabilitiesAndEquity,

        BigDecimal balanceCheck

) {
}
