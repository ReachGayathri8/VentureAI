package com.ventureai.dto.finance;

import java.math.BigDecimal;

public record CashFlowRowDto(

        int period,

        String label,

        BigDecimal openingCash,

        BigDecimal operatingCashFlow,

        BigDecimal investingCashFlow,

        BigDecimal financingCashFlow,

        BigDecimal netCashFlow,

        BigDecimal closingCash

) {
}
