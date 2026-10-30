package com.ventureai.dto.finance;

import java.math.BigDecimal;

public record BreakEvenDto(

        BigDecimal fixedCosts,

        BigDecimal variableCostPerUnit,

        BigDecimal sellingPricePerUnit,

        BigDecimal contributionMargin,

        Double breakEvenUnits,

        BigDecimal breakEvenRevenue

) {
}
