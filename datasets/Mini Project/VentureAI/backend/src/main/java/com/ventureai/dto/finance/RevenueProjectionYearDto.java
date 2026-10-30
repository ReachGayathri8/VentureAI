package com.ventureai.dto.finance;

import java.math.BigDecimal;

public record RevenueProjectionYearDto(

        int year,

        int customers,

        BigDecimal averageRevenuePerCustomer,

        BigDecimal totalRevenue,

        double growthRate

) {
}
