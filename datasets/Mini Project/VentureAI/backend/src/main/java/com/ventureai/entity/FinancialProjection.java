package com.ventureai.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * One row per projected period (month 1..N) for a startup: revenue,
 * expenses (COGS + opex), profit, cash flow and customer count. Powers the
 * P&L table, revenue-vs-expenses-vs-profit chart, cash flow statement and
 * cash-balance line chart, and the 3-year revenue projection view.
 */
@Entity
@Table(name = "financial_projections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialProjection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "startup_id", nullable = false)
    @JsonIgnore
    private Startup startup;

    @Column(nullable = false)
    private Integer period; // month number, 1-based

    private Integer customers;
    private BigDecimal revenue;
    private BigDecimal cogs;
    private BigDecimal grossProfit;
    private BigDecimal operatingExpenses;
    private BigDecimal netProfit;
    private BigDecimal openingCash;
    private BigDecimal closingCash;
    private BigDecimal netCashFlow;
}
