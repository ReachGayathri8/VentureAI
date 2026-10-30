package com.ventureai.dto.finance;

import java.math.BigDecimal;

/**
 * Request used by the Revenue Projection page to recalculate
 * financial projections using edited business assumptions.
 *
 * These values affect revenue/customer projections only.
 *
 * They do not overwrite the complete FinancialData record.
 */
public record FinancialAssumptionsRequest(

        /**
         * Starting monthly customer/order volume.
         *
         * Month 1 is based on this value.
         */
        Integer expectedMonthlyCustomers,

        /**
         * Average revenue generated per customer/order.
         *
         * Example:
         * ₹500 per order.
         */
        BigDecimal averageCustomerRevenue,

        /**
         * Expected annual growth rate supplied by the founder.
         *
         * Example:
         * 20.0 = 20% annual growth.
         *
         * This is a business assumption and must not be
         * confused with industry CAGR.
         */
        Double expectedGrowthRate,

        /**
         * Number of years to project.
         */
        Integer projectionPeriodYears

) {
}
