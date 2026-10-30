package com.ventureai.service.finance;

import com.ventureai.dto.finance.*;
import com.ventureai.entity.FinancialData;
import com.ventureai.entity.FinancialProjection;
import com.ventureai.entity.Startup;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class FinanceEngineService {

    private static final double EPS = 1e-9;

    /*
     * VentureAI financial model
     *
     * Revenue
     * -> COGS
     * -> Gross Profit
     * -> Operating Expenses
     * -> EBITDA
     * -> Depreciation
     * -> EBIT
     * -> Interest
     * -> PBT
     * -> Tax
     * -> Net Profit
     *
     * Cash:
     * Operating CF + Investing CF + Financing CF = Net CF
     * Opening Cash + Net CF = Closing Cash
     *
     * The engine never invents COGS, tax, CAC, churn or working-capital
     * percentages. Missing assumptions are treated as zero.
     */
    public List<FinancialProjection> computeMonthlyProjections(
            Startup startup,
            FinancialData fd,
            int totalMonths) {

        List<FinancialProjection> rows = new ArrayList<>();

        if (startup == null || fd == null || totalMonths <= 0) {
            return rows;
        }

        double startingCustomers =
                Math.max(0, nz(fd.getExpectedMonthlyCustomers()));

        double newCustomers =
                Math.max(0, nz(fd.getMonthlyNewCustomers()));

        double churn =
                pct(fd.getMonthlyChurnRate());

        double annualGrowth =
                fd.getExpectedGrowthRate() == null
                        ? 0
                        : fd.getExpectedGrowthRate();

        annualGrowth =
                clamp(annualGrowth, -99, 1000);

        double monthlyGrowth =
                Math.pow(
                        1 + annualGrowth / 100.0,
                        1.0 / 12.0
                ) - 1;

        double revenuePerCustomer =
                positive(fd.getSellingPricePerUnit())
                        ? nzd(fd.getSellingPricePerUnit())
                        : nzd(fd.getAverageCustomerRevenue());

        double variableCost =
                Math.max(
                        0,
                        nzd(fd.getVariableCostPerUnit())
                );

        double itemizedOpex =
                Math.max(
                        0,
                        nzd(fd.getEmployeeCost())
                )
                + Math.max(
                        0,
                        nzd(fd.getMarketingCost())
                )
                + Math.max(
                        0,
                        nzd(fd.getInfrastructureCost())
                )
                + Math.max(
                        0,
                        nzd(fd.getProductDevelopmentCost())
                );

        double monthlyOpex =
                itemizedOpex > EPS
                        ? itemizedOpex
                        : Math.max(
                                0,
                                nzd(fd.getMonthlyOperatingCost())
                        );

        double taxRate =
                pct(fd.getTaxRate());

        double debt =
                Math.max(
                        0,
                        nzd(fd.getDebtAmount())
                );

        double annualInterest =
                Math.max(
                        0,
                        nzd(fd.getAnnualInterestRate())
                );

        double monthlyInterest =
                annualInterest / 100.0 / 12.0;

        int debtTerm =
                positiveInt(fd.getDebtTermMonths());

        double capex =
                Math.max(
                        0,
                        nzd(fd.getCapitalExpenditure())
                );

        int usefulLife =
                positiveInt(fd.getAssetUsefulLifeMonths());

        double monthlyDepreciation =
                capex > EPS && usefulLife > 0
                        ? capex / usefulLife
                        : 0;

        double arRate =
                pct(fd.getAccountsReceivableRate());

        double apRate =
                pct(fd.getAccountsPayableRate());

        double inventoryRate =
                pct(fd.getInventoryRate());

        double totalFunding =
                resolveTotalFunding(fd);

        double customers =
                startingCustomers;

        /*
         * Cash starts at zero.
         * Initial/founder/external/debt funding enters through
         * financing cash flow in Month 1.
         */
        double openingCash = 0;

        double previousWorkingCapital = 0;

        double remainingDebt = debt;

        for (int month = 1; month <= totalMonths; month++) {

            /*
             * Customer projection
             */
            if (month > 1) {

                if (newCustomers > 0) {

                    customers =
                            customers
                            + newCustomers
                            - customers * churn / 100.0;

                } else {

                    customers =
                            customers * (1 + monthlyGrowth);
                }

                customers =
                        Math.max(0, customers);
            }

            /*
             * Revenue and variable costs
             */
            double revenue =
                    Math.max(
                            0,
                            customers * revenuePerCustomer
                    );

            double cogs =
                    Math.max(
                            0,
                            customers * variableCost
                    );

            double grossProfit =
                    revenue - cogs;

            /*
             * EBITDA
             */
            double ebitda =
                    grossProfit - monthlyOpex;

            /*
             * Depreciation
             */
            double depreciation =
                    month <= usefulLife
                            ? monthlyDepreciation
                            : 0;

            /*
             * EBIT
             */
            double ebit =
                    ebitda - depreciation;

            /*
             * Interest
             */
            double interest = 0;

            if (remainingDebt > EPS
                    && monthlyInterest > 0) {

                interest =
                        remainingDebt * monthlyInterest;
            }

            /*
             * Principal repayment
             */
            double principal = 0;

            if (remainingDebt > EPS
                    && debtTerm > 0
                    && month <= debtTerm) {

                principal =
                        Math.min(
                                remainingDebt,
                                debt / debtTerm
                        );

                remainingDebt =
                        Math.max(
                                0,
                                remainingDebt - principal
                        );
            }

            /*
             * Profit Before Tax
             */
            double pbt =
                    ebit - interest;

            /*
             * Tax is charged only when PBT is positive.
             */
            double tax =
                    pbt > 0
                            ? pbt * taxRate / 100.0
                            : 0;

            /*
             * Net Profit
             */
            double netProfit =
                    pbt - tax;

            /*
             * Working capital
             */
            double ar =
                    revenue * arRate / 100.0;

            double inventory =
                    cogs * inventoryRate / 100.0;

            double ap =
                    cogs * apRate / 100.0;

            double workingCapital =
                    ar + inventory - ap;

            double changeWorkingCapital =
                    workingCapital
                    - previousWorkingCapital;

            previousWorkingCapital =
                    workingCapital;

            /*
             * Operating Cash Flow
             *
             * Net Profit
             * + Depreciation
             * - Change in Working Capital
             */
            double operatingCF =
                    netProfit
                    + depreciation
                    - changeWorkingCapital;

            /*
             * Investing Cash Flow
             *
             * Capital expenditure occurs in Month 1.
             */
            double investingCF =
                    month == 1
                            ? -capex
                            : 0;

            /*
             * Financing Cash Flow
             *
             * Funding enters in Month 1.
             * Principal repayments reduce financing CF.
             */
            double financingCF =
                    (month == 1
                            ? totalFunding
                            : 0)
                    - principal;

            /*
             * Net Cash Flow
             */
            double netCashFlow =
                    operatingCF
                    + investingCF
                    + financingCF;

            /*
             * Closing Cash
             */
            double closingCash =
                    openingCash
                    + netCashFlow;

            rows.add(
                    FinancialProjection.builder()
                            .startup(startup)
                            .period(month)
                            .customers(
                                    safeCustomerCount(
                                            customers
                                    )
                            )
                            .revenue(
                                    bd(revenue)
                            )
                            .cogs(
                                    bd(cogs)
                            )
                            .grossProfit(
                                    bd(grossProfit)
                            )
                            .operatingExpenses(
                                    bd(monthlyOpex)
                            )
                            .netProfit(
                                    bd(netProfit)
                            )
                            .openingCash(
                                    bd(openingCash)
                            )
                            .closingCash(
                                    bd(closingCash)
                            )
                            .netCashFlow(
                                    bd(netCashFlow)
                            )
                            .build()
            );

            openingCash =
                    closingCash;
        }

        return rows;
    }

    public FinancialSummaryDto summarize(
            FinancialData fd,
            List<FinancialProjection> rows) {

        if (rows == null || rows.isEmpty()) {

            return new FinancialSummaryDto(
                    bd(0),
                    bd(0),
                    bd(0),
                    bd(0),
                    bd(0),
                    bd(0),
                    bd(0),
                    bd(0),
                    bd(0),
                    0,
                    0,
                    0,
                    bd(0),
                    null,
                    bd(0),
                    bd(0),
                    0
            );
        }

        FinancialProjection m1 =
                rows.get(0);

        double monthlyRevenue =
                nzd(m1.getRevenue());

        double monthlyOpex =
                nzd(m1.getOperatingExpenses());

        double annualRevenue =
                rows.stream()
                        .limit(12)
                        .mapToDouble(
                                r -> nzd(r.getRevenue())
                        )
                        .sum();

        double annualProfit =
                rows.stream()
                        .limit(12)
                        .mapToDouble(
                                r -> nzd(r.getNetProfit())
                        )
                        .sum();

        DetailedPnl p1 =
                detailedPnl(
                        fd,
                        m1.getPeriod(),
                        m1
                );

        double grossMargin =
                monthlyRevenue > EPS
                        ? p1.grossProfit
                        / monthlyRevenue
                        * 100
                        : 0;

        double profitMargin =
                annualRevenue > EPS
                        ? annualProfit
                        / annualRevenue
                        * 100
                        : 0;

        double investment =
                resolveTotalInvestment(fd);

        double roi =
                investment > EPS
                        ? annualProfit
                        / investment
                        * 100
                        : 0;

        /*
         * Monthly burn is based on operating cash burn, not total net
         * cash flow. Funding and investing transactions must not make
         * the business look artificially healthier or worse.
         */
        double burn =
                calculateAverageMonthlyBurn(
                        fd,
                        rows
                );

        double runway =
                calculateRunway(
                        rows
                );

        double closingCash =
                nzd(
                        rows.get(
                                rows.size() - 1
                        ).getClosingCash()
                );

        double endingDebt =
                calculateEndingDebt(
                        fd,
                        rows.size()
                );

        double health =
                financialHealth(
                        annualRevenue,
                        annualProfit,
                        grossMargin,
                        runway,
                        burn,
                        closingCash
                );

        return new FinancialSummaryDto(
                bd(monthlyRevenue),
                bd(annualRevenue),
                bd(monthlyOpex),
                bd(p1.grossProfit),
                bd(p1.ebitda),
                bd(p1.ebit),
                bd(p1.pbt),
                bd(p1.tax),
                bd(p1.netProfit),
                round1(grossMargin),
                round1(profitMargin),
                round1(roi),
                bd(burn),
                runway < 0
                        ? null
                        : round1(runway),
                bd(closingCash),
                bd(endingDebt),
                round1(health)
        );
    }

    public BreakEvenDto breakEven(
            FinancialData fd) {

        double fixedCosts =
                Math.max(
                        0,
                        nzd(fd.getFixedCosts())
                );

        if (fixedCosts <= EPS) {

            fixedCosts =
                    Math.max(
                            0,
                            nzd(fd.getEmployeeCost())
                    )
                    + Math.max(
                            0,
                            nzd(fd.getMarketingCost())
                    )
                    + Math.max(
                            0,
                            nzd(fd.getInfrastructureCost())
                    )
                    + Math.max(
                            0,
                            nzd(fd.getProductDevelopmentCost())
                    );
        }

        if (fixedCosts <= EPS) {

            fixedCosts =
                    Math.max(
                            0,
                            nzd(
                                    fd.getMonthlyOperatingCost()
                            )
                    );
        }

        double price =
                positive(
                        fd.getSellingPricePerUnit()
                )
                        ? nzd(
                                fd.getSellingPricePerUnit()
                        )
                        : nzd(
                                fd.getAverageCustomerRevenue()
                        );

        double variable =
                Math.max(
                        0,
                        nzd(
                                fd.getVariableCostPerUnit()
                        )
                );

        double contribution =
                price - variable;

        if (fixedCosts <= EPS
                || price <= EPS
                || contribution <= EPS) {

            return new BreakEvenDto(
                    bd(fixedCosts),
                    bd(variable),
                    bd(price),
                    bd(contribution),
                    null,
                    null
            );
        }

        double units =
                fixedCosts / contribution;

        return new BreakEvenDto(
                bd(fixedCosts),
                bd(variable),
                bd(price),
                bd(contribution),
                round1(units),
                bd(units * price)
        );
    }

    /*
     * ============================================================
     * BALANCE SHEET
     * ============================================================
     *
     * Balance Sheet is presented as of the requested snapshot month
     * (Year 1 = Month 12 by default from buildDashboard - see below).
     *
     * If projected cash becomes negative, negative cash is NOT
     * reported as a negative asset.
     *
     * Instead:
     *
     *     Cash = 0
     *     Projected funding gap = additional liability
     *
     * This represents the financing required to cover the
     * projected liquidity deficit as of that snapshot month, kept
     * separate from any actual debt the founder entered.
     */
    /**
     * Builds the balance sheet as of a specific projection month
     * ("snapshotPeriod"), rather than always the end of the full
     * projection period.
     *
     * A 3-year (36-month) projection's "Year 1 snapshot" must use Month
     * 12, never Month 36 - otherwise a business that is still burning
     * cash mid-plan gets a Year-1-labeled balance sheet built from its
     * worst, latest-period numbers. Callers that want the full-period
     * (e.g. Month 36) figures should pass rows.size() explicitly, or use
     * maxProjectedFinancingRequirement(rows) for the financing-gap figure
     * specifically.
     */
    public BalanceSheetDto balanceSheet(
            FinancialData fd,
            List<FinancialProjection> rows,
            int snapshotPeriod) {

        if (rows == null || rows.isEmpty()) {

            return new BalanceSheetDto(
                    bd(0), // cash
                    bd(0), // accounts receivable
                    bd(0), // inventory
                    bd(0), // net fixed assets
                    bd(0), // total assets

                    bd(0), // debt (existingDebt + projectedFundingGap)
                    bd(0), // existing debt
                    bd(0), // projected funding gap
                    bd(0), // accounts payable
                    bd(0), // total liabilities

                    bd(0), // founder capital
                    bd(0), // retained earnings
                    bd(0), // total equity

                    bd(0), // total liabilities + equity
                    bd(0)  // balance check
            );
        }

        /*
         * Clamp the requested snapshot period to the rows actually
         * available (e.g. a 1-year plan still works if 12 is requested).
         */
        int clampedPeriod =
                Math.max(
                        1,
                        Math.min(
                                snapshotPeriod <= 0 ? rows.size() : snapshotPeriod,
                                rows.size()
                        )
                );

        FinancialProjection latest =
                rows.stream()
                        .filter(r -> r.getPeriod() == clampedPeriod)
                        .findFirst()
                        .orElse(rows.get(clampedPeriod - 1));

        /*
         * Raw projected cash.
         */
        double rawCash =
                nzd(
                        latest.getClosingCash()
                );

        /*
         * Cash cannot be a negative asset.
         *
         * Positive cash remains an asset.
         * Negative cash becomes a financing shortfall.
         */
        double cash =
                Math.max(
                        0,
                        rawCash
                );

        double cashShortfall =
                Math.max(
                        0,
                        -rawCash
                );

        /*
         * Revenue and COGS used for working-capital balances.
         */
        double revenue =
                nzd(
                        latest.getRevenue()
                );

        double cogs =
                nzd(
                        latest.getCogs()
                );

        /*
         * Accounts Receivable.
         */
        double ar =
                revenue
                * pct(
                        fd.getAccountsReceivableRate()
                )
                / 100.0;

        /*
         * Inventory.
         */
        double inventory =
                cogs
                * pct(
                        fd.getInventoryRate()
                )
                / 100.0;

        /*
         * Accounts Payable.
         */
        double ap =
                cogs
                * pct(
                        fd.getAccountsPayableRate()
                )
                / 100.0;

        /*
         * Fixed assets.
         */
        double capex =
                Math.max(
                        0,
                        nzd(
                                fd.getCapitalExpenditure()
                        )
                );

        int usefulLife =
                positiveInt(
                        fd.getAssetUsefulLifeMonths()
                );

        int months = latest.getPeriod();

        double accumulatedDep =
                capex > EPS
                        && usefulLife > 0
                        ? Math.min(
                                capex,
                                capex
                                / usefulLife
                                * months
                        )
                        : 0;

        double netFixedAssets =
                Math.max(
                        0,
                        capex - accumulatedDep
                );

        /*
         * Scheduled debt remaining at the end of Year 1.
         */
        double scheduledDebt =
                calculateEndingDebt(
                        fd,
                        latest.getPeriod()
                );

        /*
         * Total financing liability shown by the balance-sheet DTO includes:
         *
         * 1. Remaining scheduled debt actually entered by the user
         * 2. Projected cash shortfall that would require financing
         *
         * The DTO still exposes this value through its existing `debt`
         * field for API compatibility. The UI/report should label it
         * as "Debt / Financing Requirement" rather than implying that
         * the entire amount is an existing loan.
         */
        double totalDebt =
                scheduledDebt
                + cashShortfall;

        /*
         * TOTAL ASSETS
         */
        double totalAssets =
                cash
                + ar
                + inventory
                + netFixedAssets;

        /*
         * TOTAL LIABILITIES
         */
        double totalLiabilities =
                totalDebt
                + ap;

        /*
         * Financing / equity.
         */
        double founder =
                Math.max(
                        0,
                        nzd(
                                fd.getFounderInvestment()
                        )
                );

        double external =
                Math.max(
                        0,
                        nzd(
                                fd.getExternalFunding()
                        )
                );

        double initial =
                Math.max(
                        0,
                        nzd(
                                fd.getInitialInvestment()
                        )
                );

        String financingType =
                fd.getFinancingType() == null
                        ? ""
                        : fd.getFinancingType()
                                .trim()
                                .toUpperCase();

        /*
         * External funding is equity unless the user explicitly
         * classified the financing as debt.
         */
        double externalEquity =
                isDebtFinancing(
                        financingType
                )
                        ? 0
                        : external;

        /*
         * If only Initial Investment was entered, recognize it
         * as founder capital.
         */
        if (founder <= EPS
                && externalEquity <= EPS
                && initial > EPS) {

            founder = initial;
        }

        /*
         * Retained earnings are cumulative projected net profits
         * through the balance-sheet date.
         */
        double retainedEarnings =
                rows.stream()
                        .filter(
                                r ->
                                        r.getPeriod()
                                                <= latest.getPeriod()
                        )
                        .mapToDouble(
                                r ->
                                        nzd(
                                                r.getNetProfit()
                                        )
                        )
                        .sum();

        /*
         * Total Equity.
         */
        double totalEquity =
                founder
                + externalEquity
                + retainedEarnings;

        /*
         * Accounting equation:
         *
         * Assets = Liabilities + Equity
         */
        double totalLiabilitiesAndEquity =
                totalLiabilities
                + totalEquity;

        double balanceCheck =
                totalAssets
                - totalLiabilitiesAndEquity;

        return new BalanceSheetDto(
                bd(cash),
                bd(ar),
                bd(inventory),
                bd(netFixedAssets),
                bd(totalAssets),

                bd(totalDebt),
                bd(scheduledDebt),
                bd(cashShortfall),
                bd(ap),
                bd(totalLiabilities),

                bd(founder + externalEquity),
                bd(retainedEarnings),
                bd(totalEquity),

                bd(totalLiabilitiesAndEquity),
                bd(balanceCheck)
        );
    }

    /**
     * The largest additional financing the projection implies would be
     * needed to keep cash non-negative, evaluated at the END of the full
     * projection period supplied (e.g. Month 36 of a 3-year plan).
     *
     * This is intentionally separate from balanceSheet()'s
     * projectedFundingGap, which is scoped to whatever snapshot period was
     * requested (Year 1 / Month 12 by default). A business that is still
     * mid-runway at Month 12 but recovers by Month 36 - or vice versa -
     * needs both numbers, clearly labeled, rather than one figure quietly
     * standing in for the other.
     */
    public BigDecimal maxProjectedFinancingRequirement(
            List<FinancialProjection> rows) {

        if (rows == null || rows.isEmpty()) {
            return bd(0);
        }

        double worstClosingCash =
                rows.stream()
                        .mapToDouble(r -> nzd(r.getClosingCash()))
                        .min()
                        .orElse(0);

        return bd(Math.max(0, -worstClosingCash));
    }

    public List<RevenueProjectionYearDto> revenueProjectionYearly(
            FinancialData fd,
            List<FinancialProjection> rows,
            int years) {

        List<RevenueProjectionYearDto> result =
                new ArrayList<>();

        if (rows == null
                || rows.isEmpty()
                || years <= 0) {

            return result;
        }

        double growth =
                fd.getExpectedGrowthRate() == null
                        ? 0
                        : fd.getExpectedGrowthRate();

        for (int year = 1;
             year <= years;
             year++) {

            int from =
                    (year - 1) * 12;

            int to =
                    Math.min(
                            year * 12,
                            rows.size()
                    );

            if (from >= rows.size()) {
                break;
            }

            List<FinancialProjection> yr =
                    rows.subList(
                            from,
                            to
                    );

            double revenue =
                    yr.stream()
                            .mapToDouble(
                                    r ->
                                            nzd(
                                                    r.getRevenue()
                                            )
                            )
                            .sum();

            double avgCustomers =
                    yr.stream()
                            .mapToInt(
                                    r ->
                                            r.getCustomers() == null
                                                    ? 0
                                                    : r.getCustomers()
                            )
                            .average()
                            .orElse(0);

            double customerMonths =
                    yr.stream()
                            .mapToDouble(
                                    r ->
                                            r.getCustomers() == null
                                                    ? 0
                                                    : r.getCustomers()
                            )
                            .sum();

            double arpc =
                    customerMonths > EPS
                            ? revenue
                            / customerMonths
                            : 0;

            result.add(
                    new RevenueProjectionYearDto(
                            year,
                            (int) Math.round(
                                    avgCustomers
                            ),
                            bd(arpc),
                            bd(revenue),
                            growth
                    )
            );
        }

        return result;
    }

    public FinancialDashboardResponse buildDashboard(
            Startup startup,
            FinancialData fd,
            List<FinancialProjection> rows) {

        final List<FinancialProjection> projectionRows =
                rows == null
                        ? new ArrayList<>()
                        : rows;

        /*
         * P&L
         */
        List<PnLRowDto> pnl =
                projectionRows.stream()
                        .map(
                                r -> {

                                    DetailedPnl p =
                                            detailedPnl(
                                                    fd,
                                                    r.getPeriod(),
                                                    r
                                            );

                                    return new PnLRowDto(
                                            r.getPeriod(),
                                            "Month "
                                                    + r.getPeriod(),
                                            r.getRevenue(),
                                            r.getCogs(),
                                            r.getGrossProfit(),
                                            r.getOperatingExpenses(),
                                            bd(
                                                    p.depreciation
                                            ),
                                            bd(
                                                    p.ebit
                                            ),
                                            bd(
                                                    p.interest
                                            ),
                                            bd(
                                                    p.pbt
                                            ),
                                            bd(
                                                    p.tax
                                            ),
                                            r.getNetProfit()
                                    );
                                }
                        )
                        .toList();

        double capex =
                Math.max(
                        0,
                        nzd(
                                fd.getCapitalExpenditure()
                        )
                );

        double funding =
                resolveTotalFunding(fd);

        /*
         * Cash Flow
         */
        List<CashFlowRowDto> cashFlow =
                projectionRows.stream()
                        .map(
                                r -> {

                                    DetailedPnl p =
                                            detailedPnl(
                                                    fd,
                                                    r.getPeriod(),
                                                    r
                                            );

                                    double wcChange =
                                            workingCapitalChange(
                                                    fd,
                                                    projectionRows,
                                                    r.getPeriod()
                                            );

                                    double operatingCF =
                                            p.netProfit
                                            + p.depreciation
                                            - wcChange;

                                    double investingCF =
                                            r.getPeriod() == 1
                                                    ? -capex
                                                    : 0;

                                    double principal =
                                            principalRepayment(
                                                    fd,
                                                    r.getPeriod()
                                            );

                                    double financingCF =
                                            (
                                                    r.getPeriod() == 1
                                                            ? funding
                                                            : 0
                                            )
                                            - principal;

                                    double netCF =
                                            operatingCF
                                            + investingCF
                                            + financingCF;

                                    return new CashFlowRowDto(
                                            r.getPeriod(),
                                            "Month "
                                                    + r.getPeriod(),
                                            r.getOpeningCash(),
                                            bd(
                                                    operatingCF
                                            ),
                                            bd(
                                                    investingCF
                                            ),
                                            bd(
                                                    financingCF
                                            ),
                                            bd(
                                                    netCF
                                            ),
                                            r.getClosingCash()
                                    );
                                }
                        )
                        .toList();

        int years =
                fd.getProjectionPeriodYears() == null
                        ? 3
                        : Math.max(
                                1,
                                fd.getProjectionPeriodYears()
                        );

        /*
         * "Balance Sheet" on the dashboard/report is the Year 1 (Month 12)
         * snapshot, not the end of the full multi-year projection. See
         * maxProjectedFinancingRequirement below for the full-period figure.
         */
        int year1SnapshotPeriod =
                projectionRows.isEmpty()
                        ? 0
                        : Math.min(12, projectionRows.size());

        return new FinancialDashboardResponse(
                summarize(
                        fd,
                        projectionRows
                ),
                breakEven(fd),
                balanceSheet(
                        fd,
                        projectionRows,
                        year1SnapshotPeriod
                ),
                pnl,
                cashFlow,
                revenueProjectionYearly(
                        fd,
                        projectionRows,
                        years
                ),
                maxProjectedFinancingRequirement(
                        projectionRows
                )
        );
    }

    private DetailedPnl detailedPnl(
            FinancialData fd,
            int period,
            FinancialProjection row) {

        double revenue =
                nzd(
                        row.getRevenue()
                );

        double cogs =
                nzd(
                        row.getCogs()
                );

        double gross =
                nzd(
                        row.getGrossProfit()
                );

        double opex =
                nzd(
                        row.getOperatingExpenses()
                );

        double depreciation =
                depreciation(
                        fd,
                        period
                );

        double ebitda =
                gross - opex;

        double ebit =
                ebitda - depreciation;

        double interest =
                interestExpense(
                        fd,
                        period
                );

        double pbt =
                ebit - interest;

        double tax =
                pbt > 0
                        ? pbt
                        * pct(fd.getTaxRate())
                        / 100
                        : 0;

        double net =
                pbt - tax;

        return new DetailedPnl(
                revenue,
                cogs,
                gross,
                opex,
                depreciation,
                ebitda,
                ebit,
                interest,
                pbt,
                tax,
                net
        );
    }

    private double depreciation(
            FinancialData fd,
            int period) {

        double capex =
                Math.max(
                        0,
                        nzd(
                                fd.getCapitalExpenditure()
                        )
                );

        int life =
                positiveInt(
                        fd.getAssetUsefulLifeMonths()
                );

        return capex > EPS
                && life > 0
                && period <= life
                ? capex / life
                : 0;
    }

    private double interestExpense(
            FinancialData fd,
            int period) {

        double debt =
                Math.max(
                        0,
                        nzd(
                                fd.getDebtAmount()
                        )
                );

        double rate =
                Math.max(
                        0,
                        nzd(
                                fd.getAnnualInterestRate()
                        )
                );

        int term =
                positiveInt(
                        fd.getDebtTermMonths()
                );

        if (debt <= EPS
                || rate <= EPS
                || term <= 0
                || period > term) {

            return 0;
        }

        double repaidBefore =
                debt
                * Math.min(
                        period - 1,
                        term
                )
                / term;

        double openingDebt =
                Math.max(
                        0,
                        debt - repaidBefore
                );

        return openingDebt
                * rate
                / 100
                / 12;
    }

    private double principalRepayment(
            FinancialData fd,
            int period) {

        double debt =
                Math.max(
                        0,
                        nzd(
                                fd.getDebtAmount()
                        )
                );

        int term =
                positiveInt(
                        fd.getDebtTermMonths()
                );

        if (debt <= EPS
                || term <= 0
                || period > term) {

            return 0;
        }

        return debt / term;
    }

    private double calculateEndingDebt(
            FinancialData fd,
            int months) {

        double debt =
                Math.max(
                        0,
                        nzd(
                                fd.getDebtAmount()
                        )
                );

        int term =
                positiveInt(
                        fd.getDebtTermMonths()
                );

        if (debt <= EPS
                || term <= 0) {

            return debt;
        }

        double repaid =
                debt
                * Math.min(
                        months,
                        term
                )
                / term;

        return Math.max(
                0,
                debt - repaid
        );
    }

    private double workingCapitalChange(
            FinancialData fd,
            List<FinancialProjection> rows,
            int period) {

        FinancialProjection current =
                rows.stream()
                        .filter(
                                r ->
                                        r.getPeriod()
                                                == period
                        )
                        .findFirst()
                        .orElse(null);

        if (current == null) {
            return 0;
        }

        double currentWC =
                workingCapital(
                        fd,
                        current
                );

        double previousWC = 0;

        if (period > 1) {

            FinancialProjection previous =
                    rows.stream()
                            .filter(
                                    r ->
                                            r.getPeriod()
                                                    == period - 1
                            )
                            .findFirst()
                            .orElse(null);

            if (previous != null) {

                previousWC =
                        workingCapital(
                                fd,
                                previous
                        );
            }
        }

        return currentWC
                - previousWC;
    }

    private double workingCapital(
            FinancialData fd,
            FinancialProjection row) {

        double revenue =
                nzd(
                        row.getRevenue()
                );

        double cogs =
                nzd(
                        row.getCogs()
                );

        double ar =
                revenue
                * pct(
                        fd.getAccountsReceivableRate()
                )
                / 100;

        double inventory =
                cogs
                * pct(
                        fd.getInventoryRate()
                )
                / 100;

        double ap =
                cogs
                * pct(
                        fd.getAccountsPayableRate()
                )
                / 100;

        return ar
                + inventory
                - ap;
    }

    /**
     * Average monthly operating cash burn.
     *
     * Burn is calculated from operating cash flow only:
     *   Net Profit + Depreciation - Change in Working Capital
     *
     * Financing inflows, debt principal repayments and capital expenditure
     * are intentionally excluded from the burn metric. This keeps burn as
     * an operating-health measure and prevents funding from masking the
     * underlying monthly cash consumption.
     */
    private double calculateAverageMonthlyBurn(
            FinancialData fd,
            List<FinancialProjection> rows) {

        if (fd == null
                || rows == null
                || rows.isEmpty()) {

            return 0;
        }

        double totalBurn = 0;
        int burnMonths = 0;

        for (FinancialProjection row : rows) {

            int period = row.getPeriod();

            DetailedPnl pnl =
                    detailedPnl(
                            fd,
                            period,
                            row
                    );

            double changeWorkingCapital =
                    workingCapitalChange(
                            fd,
                            rows,
                            period
                    );

            double operatingCashFlow =
                    pnl.netProfit
                    + pnl.depreciation
                    - changeWorkingCapital;

            if (operatingCashFlow < -EPS) {

                totalBurn += -operatingCashFlow;
                burnMonths++;
            }
        }

        return burnMonths > 0
                ? totalBurn / burnMonths
                : 0;
    }

    /**
     * Projected cash runway: how long the startup's actual projected cash
     * balance lasts before it is exhausted.
     *
     * This is intentionally NOT "starting funding / average burn" - that
     * flat estimate ignores that revenue and costs both change month to
     * month, and can make the runway look far longer than the month-by-
     * month cash trajectory actually shows. Instead, this scans the real
     * projection rows for the first month whose closing cash reaches zero
     * or below, and linearly interpolates a fractional month within that
     * month using its own opening/closing cash. Financing inflows are not
     * added on top of this - they are already reflected inside each row's
     * cash balance (they entered as financing cash flow in Month 1), so
     * they naturally extend runway only to the extent they actually would.
     *
     * Returns -1 (by the same convention as the caller) if cash never
     * reaches zero or below within the projected period.
     */
    private double calculateRunway(
            List<FinancialProjection> rows) {

        if (rows == null || rows.isEmpty()) {
            return -1;
        }

        for (FinancialProjection row : rows) {

            double opening = nzd(row.getOpeningCash());
            double closing = nzd(row.getClosingCash());

            if (closing > EPS) {
                continue;
            }

            int monthsAlreadyElapsed = row.getPeriod() - 1;

            if (opening <= EPS) {
                /*
                 * Cash was already exhausted before this month began.
                 */
                return Math.max(0, monthsAlreadyElapsed);
            }

            double declineThisMonth = opening - closing;

            double fractionOfMonth =
                    declineThisMonth > EPS
                            ? clamp(opening / declineThisMonth, 0, 1)
                            : 1.0;

            return monthsAlreadyElapsed + fractionOfMonth;
        }

        /*
         * Cash never goes to zero or below within the projected period.
         */
        return -1;
    }

    /*
     * Transparent financial health score.
     */
    private double financialHealth(
            double annualRevenue,
            double annualProfit,
            double grossMargin,
            double runway,
            double burn,
            double closingCash) {

        double profitMargin =
                annualRevenue > EPS
                        ? annualProfit
                        / annualRevenue
                        * 100
                        : -100;

        double profitability =
                clamp(
                        50
                        + profitMargin * 2,
                        0,
                        100
                );

        double gross =
                clamp(
                        grossMargin,
                        0,
                        100
                );

        double liquidity;

        if (runway < 0) {

            liquidity = 90;

        } else {

            liquidity =
                    clamp(
                            runway * 10,
                            0,
                            100
                    );
        }

        double cash =
                closingCash > EPS
                        ? 100
                        : 20;

        double burnScore =
                burn <= EPS
                        ? 100
                        : (
                                closingCash > EPS
                                        ? 70
                                        : 20
                        );

        return clamp(
                profitability * .30
                + gross * .20
                + liquidity * .25
                + cash * .10
                + burnScore * .15,
                0,
                100
        );
    }

    private double resolveTotalFunding(
            FinancialData fd) {

        double founder =
                Math.max(
                        0,
                        nzd(
                                fd.getFounderInvestment()
                        )
                );

        double external =
                Math.max(
                        0,
                        nzd(
                                fd.getExternalFunding()
                        )
                );

        double debt =
                Math.max(
                        0,
                        nzd(
                                fd.getDebtAmount()
                        )
                );

        double explicit =
                founder
                + external
                + debt;

        return explicit > EPS
                ? explicit
                : Math.max(
                        0,
                        nzd(
                                fd.getInitialInvestment()
                        )
                );
    }

    private double resolveTotalInvestment(
            FinancialData fd) {

        return resolveTotalFunding(fd);
    }

    private boolean isDebtFinancing(
            String type) {

        if (type == null) {
            return false;
        }

        String v =
                type.trim()
                        .toUpperCase();

        return v.contains("DEBT")
                || v.contains("LOAN")
                || v.contains("BORROW");
    }

    private double nz(Integer v) {
        return v == null
                ? 0
                : v;
    }

    private int positiveInt(Integer v) {
        return v == null
                ? 0
                : Math.max(
                        0,
                        v
                );
    }

    private double nzd(BigDecimal v) {
        return v == null
                ? 0
                : v.doubleValue();
    }

    private double nzd(Double v) {
        return v == null
                ? 0
                : v;
    }

    private boolean positive(
            BigDecimal v) {

        return v != null
                && v.compareTo(
                        BigDecimal.ZERO
                ) > 0;
    }

    private double pct(Double v) {

        return v == null
                ? 0
                : clamp(
                        v,
                        0,
                        100
                );
    }

    private double clamp(
            double value,
            double min,
            double max) {

        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }

    private BigDecimal bd(
            double value) {

        if (!Double.isFinite(value)) {
            value = 0;
        }

        return BigDecimal
                .valueOf(value)
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                );
    }

    private double round1(
            double value) {

        if (!Double.isFinite(value)) {
            return 0;
        }

        return Math.round(
                value * 10
        ) / 10.0;
    }

    private int safeCustomerCount(
            double value) {

        if (!Double.isFinite(value)) {
            return 0;
        }

        return (int) Math.min(
                Integer.MAX_VALUE,
                Math.max(
                        0,
                        Math.round(value)
                )
        );
    }

    private static class DetailedPnl {

        final double revenue;
        final double cogs;
        final double grossProfit;
        final double operatingExpenses;
        final double depreciation;
        final double ebitda;
        final double ebit;
        final double interest;
        final double pbt;
        final double tax;
        final double netProfit;

        DetailedPnl(
                double revenue,
                double cogs,
                double grossProfit,
                double operatingExpenses,
                double depreciation,
                double ebitda,
                double ebit,
                double interest,
                double pbt,
                double tax,
                double netProfit) {

            this.revenue =
                    revenue;

            this.cogs =
                    cogs;

            this.grossProfit =
                    grossProfit;

            this.operatingExpenses =
                    operatingExpenses;

            this.depreciation =
                    depreciation;

            this.ebitda =
                    ebitda;

            this.ebit =
                    ebit;

            this.interest =
                    interest;

            this.pbt =
                    pbt;

            this.tax =
                    tax;

            this.netProfit =
                    netProfit;
        }
    }
}