package com.ventureai.service;

import com.ventureai.dto.finance.FinancialAssumptionsRequest;
import com.ventureai.dto.finance.FinancialDashboardResponse;
import com.ventureai.dto.finance.FinancialDataRequest;
import com.ventureai.entity.FinancialData;
import com.ventureai.entity.FinancialProjection;
import com.ventureai.entity.Startup;
import com.ventureai.entity.User;
import com.ventureai.exception.BadRequestException;
import com.ventureai.repository.FinancialDataRepository;
import com.ventureai.repository.FinancialProjectionRepository;
import com.ventureai.service.finance.FinanceEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinanceQueryService {

    private final StartupService startupService;
    private final FinancialDataRepository financialDataRepository;
    private final FinancialProjectionRepository financialProjectionRepository;
    private final FinanceEngineService financeEngineService;

    // ============================================================
    // FINANCIAL DASHBOARD
    // ============================================================

    @Transactional(readOnly = true)
    public FinancialDashboardResponse getDashboard(
            User user,
            Long startupId
    ) {

        Startup startup =
                startupService.getOwnedStartup(
                        user,
                        startupId
                );

        FinancialData fd =
                financialDataRepository
                        .findByStartup(startup)
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Financial inputs are incomplete for this startup."
                                )
                        );

        List<FinancialProjection> rows =
                financialProjectionRepository
                        .findByStartupOrderByPeriodAsc(startup);

        /*
         * If projections have not yet been generated,
         * calculate them temporarily for dashboard display.
         */
        if (rows.isEmpty()) {

            int years =
                    fd.getProjectionPeriodYears() == null
                            ? 3
                            : fd.getProjectionPeriodYears();

            int months = years * 12;

            rows =
                    financeEngineService.computeMonthlyProjections(
                            startup,
                            fd,
                            months
                    );
        }

        return financeEngineService.buildDashboard(
                startup,
                fd,
                rows
        );
    }


    // ============================================================
    // WHAT-IF FINANCIAL RECALCULATION
    // ============================================================

    @Transactional
    public FinancialDashboardResponse recalculate(
            User user,
            Long startupId,
            FinancialAssumptionsRequest assumptions
    ) {

        Startup startup =
                startupService.getOwnedStartup(
                        user,
                        startupId
                );

        FinancialData fd =
                financialDataRepository
                        .findByStartup(startup)
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Financial inputs are incomplete for this startup."
                                )
                        );

        /*
         * These values are intentionally temporary.
         *
         * This endpoint is used for "what-if" calculations
         * on the Revenue Projection page.
         *
         * The database is NOT modified.
         */

        if (assumptions.expectedMonthlyCustomers() != null) {

            fd.setExpectedMonthlyCustomers(
                    assumptions.expectedMonthlyCustomers()
            );
        }

        if (assumptions.averageCustomerRevenue() != null) {

            fd.setAverageCustomerRevenue(
                    assumptions.averageCustomerRevenue()
            );
        }

        if (assumptions.expectedGrowthRate() != null) {

            fd.setExpectedGrowthRate(
                    assumptions.expectedGrowthRate()
            );
        }

        if (assumptions.projectionPeriodYears() != null) {

            fd.setProjectionPeriodYears(
                    assumptions.projectionPeriodYears()
            );
        }


        int years =
                fd.getProjectionPeriodYears() == null
                        ? 3
                        : fd.getProjectionPeriodYears();

        int months = years * 12;


        List<FinancialProjection> rows =
                financeEngineService.computeMonthlyProjections(
                        startup,
                        fd,
                        months
                );


        return financeEngineService.buildDashboard(
                startup,
                fd,
                rows
        );
    }


    // ============================================================
    // SAVE FINANCIAL DATA
    // ============================================================

    @Transactional
    public void updateFinancialData(
            User user,
            Long startupId,
            FinancialDataRequest req
    ) {

        if (req == null) {

            throw new BadRequestException(
                    "Financial data is required."
            );
        }


        Startup startup =
                startupService.getOwnedStartup(
                        user,
                        startupId
                );


        FinancialData fd =
                financialDataRepository
                        .findByStartup(startup)
                        .orElseGet(() ->
                                FinancialData.builder()
                                        .startup(startup)
                                        .build()
                        );


        // ========================================================
        // FUNDING
        // ========================================================

        fd.setInitialInvestment(
                nz(req.initialInvestment())
        );

        fd.setFounderInvestment(
                nz(req.founderInvestment())
        );

        fd.setExternalFunding(
                nz(req.externalFunding())
        );

        if (req.financingType() != null
                && !req.financingType().isBlank()) {

            fd.setFinancingType(
                    req.financingType().trim()
            );
        }


        // ========================================================
        // OPERATING COSTS
        // ========================================================

        fd.setMonthlyOperatingCost(
                nz(req.monthlyOperatingCost())
        );

        fd.setEmployeeCost(
                nz(req.employeeCost())
        );

        fd.setMarketingCost(
                nz(req.marketingCost())
        );

        fd.setInfrastructureCost(
                nz(req.infrastructureCost())
        );

        fd.setProductDevelopmentCost(
                nz(req.productDevelopmentCost())
        );


        // ========================================================
        // CUSTOMER / REVENUE ASSUMPTIONS
        // ========================================================

        fd.setExpectedMonthlyCustomers(
                nonNegativeInteger(
                        req.expectedMonthlyCustomers()
                )
        );

        fd.setAverageCustomerRevenue(
                nz(req.averageCustomerRevenue())
        );

        fd.setExpectedGrowthRate(
                req.expectedGrowthRate() == null
                        ? 0.0
                        : req.expectedGrowthRate()
        );

        fd.setProjectionPeriodYears(
                req.projectionPeriodYears() == null
                        ? 3
                        : req.projectionPeriodYears()
        );


        // ========================================================
        // UNIT ECONOMICS
        // ========================================================

        fd.setFixedCosts(
                nz(req.fixedCosts())
        );

        fd.setVariableCostPerUnit(
                nz(req.variableCostPerUnit())
        );

        fd.setSellingPricePerUnit(
                nz(req.sellingPricePerUnit())
        );

        fd.setMonthlyNewCustomers(
                nonNegativeInteger(
                        req.monthlyNewCustomers()
                )
        );

        fd.setCustomerAcquisitionCost(
                nz(req.customerAcquisitionCost())
        );


        // ========================================================
        // CUSTOMER RETENTION
        // ========================================================

        if (req.monthlyChurnRate() != null) {

            fd.setMonthlyChurnRate(
                    req.monthlyChurnRate()
            );
        }

        if (req.averageCustomerLifetimeMonths() != null) {

            fd.setAverageCustomerLifetimeMonths(
                    req.averageCustomerLifetimeMonths()
            );
        }


        // ========================================================
        // TAX
        // ========================================================

        if (req.taxRate() != null) {

            fd.setTaxRate(
                    req.taxRate()
            );
        }


        // ========================================================
        // CAPITAL EXPENDITURE
        // ========================================================

        fd.setCapitalExpenditure(
                nz(req.capitalExpenditure())
        );

        if (req.assetUsefulLifeMonths() != null) {

            fd.setAssetUsefulLifeMonths(
                    req.assetUsefulLifeMonths()
            );
        }


        // ========================================================
        // DEBT
        // ========================================================

        fd.setDebtAmount(
                nz(req.debtAmount())
        );

        if (req.annualInterestRate() != null) {

            fd.setAnnualInterestRate(
                    req.annualInterestRate()
            );
        }

        if (req.debtTermMonths() != null) {

            fd.setDebtTermMonths(
                    req.debtTermMonths()
            );
        }


        // ========================================================
        // WORKING CAPITAL
        // ========================================================

        if (req.accountsReceivableRate() != null) {

            fd.setAccountsReceivableRate(
                    req.accountsReceivableRate()
            );
        }

        if (req.accountsPayableRate() != null) {

            fd.setAccountsPayableRate(
                    req.accountsPayableRate()
            );
        }

        if (req.inventoryRate() != null) {

            fd.setInventoryRate(
                    req.inventoryRate()
            );
        }


        // ========================================================
        // CASH RESERVE
        // ========================================================

        fd.setMinimumCashReserve(
                nz(req.minimumCashReserve())
        );


        // ========================================================
        // SAVE
        // ========================================================

        financialDataRepository.save(fd);
    }


    // ============================================================
    // BIGDECIMAL NULL HANDLING
    // ============================================================

    private BigDecimal nz(BigDecimal value) {

        return value == null
                ? BigDecimal.ZERO
                : value;
    }


    // ============================================================
    // INTEGER NULL HANDLING
    // ============================================================

    private Integer nonNegativeInteger(Integer value) {

        if (value == null) {
            return 0;
        }

        return Math.max(0, value);
    }
}
