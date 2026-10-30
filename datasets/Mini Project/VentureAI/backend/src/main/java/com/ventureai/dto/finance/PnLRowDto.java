package com.ventureai.dto.finance;

import java.math.BigDecimal;

public record PnLRowDto(

        int period,

        String label,

        BigDecimal revenue,

        BigDecimal cogs,

        BigDecimal grossProfit,

        BigDecimal operatingExpenses,

        BigDecimal depreciation,

        BigDecimal ebit,

        BigDecimal interestExpense,

        BigDecimal profitBeforeTax,

        BigDecimal tax,

        BigDecimal netProfit

) {
}
