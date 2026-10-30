import React from 'react';
import { useParams } from 'react-router-dom';

import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  BarChart,
  Bar,
} from 'recharts';

import {
  Wallet,
  TrendingUp,
  PiggyBank,
  Gauge,
  DollarSign,
  Activity,
  Landmark,
  Scale,
  AlertTriangle,
} from 'lucide-react';

import PageHeader from '../components/PageHeader';
import Loader from '../components/Loader';
import StartupPicker from '../components/StartupPicker';
import FinancialTabs from '../components/FinancialTabs';
import KpiCard from '../components/KpiCard';
import ChartCard from '../charts/ChartCard';
import useFinanceData from '../hooks/useFinanceData';

import {
  formatCurrency,
  formatPercent,
} from '../utils/format';

export default function FinancialDashboardPage() {
  const { startupId } = useParams();

  if (!startupId) {
    return (
      <div>
        <PageHeader
          title="Financial Analysis"
          subtitle="Pick a startup to view its financial dashboard."
        />

        <StartupPicker
          toPath={(id) => `/financials/${id}`}
          title="financial dashboard"
        />
      </div>
    );
  }

  return (
    <FinancialOverview startupId={startupId} />
  );
}

function FinancialOverview({ startupId }) {
  const {
    data,
    loading,
    error,
  } = useFinanceData(startupId);

  if (loading) {
    return (
      <div>
        <PageHeader
          title="Financial Dashboard"
          subtitle="Revenue, profitability, cash flow and financial health."
        />

        <FinancialTabs startupId={startupId} />

        <Loader label="Crunching the numbers..." />
      </div>
    );
  }

  if (error) {
    return (
      <div>
        <PageHeader
          title="Financial Dashboard"
          subtitle="Revenue, profitability, cash flow and financial health."
        />

        <FinancialTabs startupId={startupId} />

        <div className="card text-sm text-brand-red">
          {error}
        </div>
      </div>
    );
  }

  if (!data || !data.summary) {
    return (
      <div>
        <PageHeader
          title="Financial Dashboard"
          subtitle="Revenue, profitability, cash flow and financial health."
        />

        <FinancialTabs startupId={startupId} />

        <div className="card text-sm text-slate-500">
          No financial data is available for this startup.
        </div>
      </div>
    );
  }

  const summary = data.summary;

  const breakEven = data.breakEven || {};
  const balanceSheet = data.balanceSheet || {};
  const pnl = data.pnlMonthly || [];
  const cashFlow = data.cashFlowMonthly || [];
  const yearlyRevenue = data.revenueProjectionYearly || [];

  /* =========================================================
     FINANCIAL HEALTH
     ========================================================= */

  const financialHealth = Number(
    summary.financialHealthScore || 0
  );

  /* =========================================================
     CASH POSITION
     ========================================================= */

  const closingCash = Number(
    summary.closingCash || 0
  );

  /*
   * Actual debt comes only from the financial engine's
   * debt calculation. This is never inflated by a projected
   * cash shortfall.
   */

  const actualDebt = Number(
    summary.totalDebt || 0
  );

  /*
   * Year 1 Funding Gap: additional funding the model implies would
   * be needed to keep cash non-negative as of the Year 1 (Month 12)
   * snapshot. Sourced from the balance sheet's projectedFundingGap -
   * deliberately NOT from summary.closingCash, which reflects the
   * final month of the full projection period (e.g. Month 36) and
   * would be inconsistent with the Year 1 balance sheet shown below.
   *
   * This is a projected financing requirement, not automatically
   * additional debt.
   */

  const yearOneFundingGap = Number(
    balanceSheet.projectedFundingGap || 0
  );

  const hasYearOneFundingGap =
    yearOneFundingGap > 0;

  /*
   * Maximum Projected Funding Requirement: the largest additional
   * financing the projection implies would be needed at any point
   * across the FULL projection period, which can be later than
   * Year 1 (e.g. Month 36 of a 3-year plan). This is distinct from
   * the Year 1 Funding Gap above and must not be confused with it.
   */

  const maxFundingRequirement = Number(
    data.maxProjectedFinancingRequirement || 0
  );

  const hasMaxFundingRequirement =
    maxFundingRequirement > 0;

  /* =========================================================
     BALANCE SHEET CHECK
     ========================================================= */

  const balanceCheck = Number(
    balanceSheet.balanceCheck || 0
  );

  const isBalanced =
    Math.abs(balanceCheck) < 0.01;

  return (
    <div className="space-y-6">

      {/* =====================================================
          HEADER
          ===================================================== */}

      <PageHeader
        title="Financial Dashboard"
        subtitle="Revenue, profitability, cash flow and financial health."
      />

      <FinancialTabs
        startupId={startupId}
      />

      {/* =====================================================
          MAIN KPI CARDS
          ===================================================== */}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">

        <KpiCard
          label="Monthly Revenue"
          value={formatCurrency(
            summary.monthlyRevenue
          )}
          icon={Wallet}
          accent="navy"
        />

        <KpiCard
          label="Annual Revenue"
          value={formatCurrency(
            summary.annualRevenue
          )}
          icon={TrendingUp}
          accent="emerald"
        />

        <KpiCard
          label="Net Profit Margin (Year 1)"
          value={formatPercent(
            summary.profitMarginPercent
          )}
          icon={Gauge}
          accent={
            Number(
              summary.profitMarginPercent || 0
            ) >= 0
              ? 'emerald'
              : 'red'
          }
        />

        <KpiCard
          label="Financial Health"
          value={`${financialHealth.toFixed(0)}/100`}
          icon={Activity}
          accent={getHealthAccent(
            financialHealth
          )}
        />

      </div>

      {/* =====================================================
          SECONDARY METRICS
          ===================================================== */}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">

        <MetricCard
          label="EBITDA"
          value={formatCurrency(
            summary.ebitdaMonth1
          )}
          icon={DollarSign}
        />

        <MetricCard
          label="EBIT"
          value={formatCurrency(
            summary.ebitMonth1
          )}
          icon={TrendingUp}
        />

        <MetricCard
          label="Closing Cash"
          value={formatCurrency(
            closingCash
          )}
          icon={PiggyBank}
          negative={closingCash < 0}
        />

        <MetricCard
          label="Actual Debt"
          value={formatCurrency(
            actualDebt
          )}
          icon={Landmark}
        />

      </div>

      {/* =====================================================
          FUNDING GAP WARNINGS

          Two distinct, separately-labeled figures:
            - Year 1 Funding Gap = Month 12 snapshot
              (balanceSheet.projectedFundingGap)
            - Maximum Projected Funding Requirement = worst
              projected cash shortfall across the full plan
              (data.maxProjectedFinancingRequirement)

          These must never be merged into one number, and neither
          is described as a loan - both are projected financing
          requirements only.
          ===================================================== */}

      {hasYearOneFundingGap && (
        <div className="card border border-amber-200 bg-amber-50">

          <div className="flex items-start gap-3">

            <AlertTriangle
              className="mt-0.5 h-5 w-5 shrink-0 text-amber-600"
            />

            <div>

              <h3 className="font-bold text-navy-900">
                Year 1 Funding Gap
              </h3>

              <p className="mt-1 text-sm text-slate-600">
                As of the Year 1 (Month 12) snapshot, projected
                cash is negative. The startup would require
                approximately{' '}
                <strong>
                  {formatCurrency(
                    yearOneFundingGap
                  )}
                </strong>{' '}
                of additional funding to maintain a
                non-negative cash balance.
              </p>

              <p className="mt-2 text-xs text-slate-500">
                This is a projected funding requirement, not
                automatically additional debt.
              </p>

            </div>

          </div>

        </div>
      )}

      {hasMaxFundingRequirement && (
        <div className="card border border-orange-200 bg-orange-50">

          <div className="flex items-start gap-3">

            <AlertTriangle
              className="mt-0.5 h-5 w-5 shrink-0 text-orange-600"
            />

            <div>

              <h3 className="font-bold text-navy-900">
                Maximum Projected Funding Requirement
              </h3>

              <p className="mt-1 text-sm text-slate-600">
                Across the full projection period, the worst
                projected cash position implies a maximum funding
                requirement of approximately{' '}
                <strong>
                  {formatCurrency(
                    maxFundingRequirement
                  )}
                </strong>.
              </p>

              <p className="mt-2 text-xs text-slate-500">
                This is a separate, later-in-time figure from the
                Year 1 Funding Gap above (Month 12) - it reflects
                the worst point across the entire plan, not
                automatically additional debt.
              </p>

            </div>

          </div>

        </div>
      )}

      {/* =====================================================
          FINANCIAL HEALTH
          ===================================================== */}

      <div className="card">

        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">

          <div>

            <h3 className="text-lg font-bold text-navy-900">
              Financial Health
            </h3>

            <p className="text-sm text-slate-500">
              Overall financial strength based on
              profitability, liquidity and capital
              structure.
            </p>

          </div>

          <div className="text-right">

            <p className="text-3xl font-extrabold text-navy-900">
              {financialHealth.toFixed(0)}

              <span className="text-base font-semibold text-slate-400">
                /100
              </span>
            </p>

            <p className="text-xs font-semibold text-slate-500">
              {getHealthLabel(
                financialHealth
              )}
            </p>

          </div>

        </div>

        <div className="mt-4 h-3 overflow-hidden rounded-full bg-slate-100">

          <div
            className="h-full rounded-full bg-navy-900 transition-all"
            style={{
              width: `${Math.min(
                100,
                Math.max(
                  0,
                  financialHealth
                )
              )}%`,
            }}
          />

        </div>

      </div>

      {/* =====================================================
          P&L CHART
          ===================================================== */}

      {pnl.length > 0 && (
        <ChartCard
          title="Revenue vs Expenses vs Profit"
          subtitle="Monthly projected financial performance"
        >

          <div className="h-[340px] w-full">

            <ResponsiveContainer
              width="100%"
              height="100%"
            >

              <LineChart
                data={pnl}
              >

                <CartesianGrid
                  strokeDasharray="3 3"
                  stroke="#eef1f8"
                />

                <XAxis
                  dataKey="label"
                  tick={{
                    fontSize: 11,
                  }}
                  interval={2}
                />

                <YAxis
                  tick={{
                    fontSize: 11,
                  }}
                  tickFormatter={(value) =>
                    formatCompactCurrency(
                      value
                    )
                  }
                />

                <Tooltip
                  formatter={(value) =>
                    formatCurrency(value)
                  }
                />

                <Legend />

                <Line
                  type="monotone"
                  dataKey="revenue"
                  stroke="#1f2c58"
                  strokeWidth={2}
                  dot={false}
                  name="Revenue"
                />

                <Line
                  type="monotone"
                  dataKey="operatingExpenses"
                  stroke="#c81e2d"
                  strokeWidth={2}
                  dot={false}
                  name="Operating Expenses"
                />

                <Line
                  type="monotone"
                  dataKey="netProfit"
                  stroke="#059669"
                  strokeWidth={2}
                  dot={false}
                  name="Net Profit"
                />

              </LineChart>

            </ResponsiveContainer>

          </div>

        </ChartCard>
      )}

      {/* =====================================================
          CASH FLOW
          ===================================================== */}

      {cashFlow.length > 0 && (
        <ChartCard
          title="Cash Flow"
          subtitle="Operating, investing and financing cash flows"
        >

          <div className="h-[320px] w-full">

            <ResponsiveContainer
              width="100%"
              height="100%"
            >

              <LineChart
                data={cashFlow}
              >

                <CartesianGrid
                  strokeDasharray="3 3"
                  stroke="#eef1f8"
                />

                <XAxis
                  dataKey="label"
                  tick={{
                    fontSize: 11,
                  }}
                  interval={2}
                />

                <YAxis
                  tick={{
                    fontSize: 11,
                  }}
                  tickFormatter={(value) =>
                    formatCompactCurrency(
                      value
                    )
                  }
                />

                <Tooltip
                  formatter={(value) =>
                    formatCurrency(value)
                  }
                />

                <Legend />

                <Line
                  type="monotone"
                  dataKey="operatingCashFlow"
                  stroke="#1f2c58"
                  strokeWidth={2}
                  dot={false}
                  name="Operating CF"
                />

                <Line
                  type="monotone"
                  dataKey="investingCashFlow"
                  stroke="#c81e2d"
                  strokeWidth={2}
                  dot={false}
                  name="Investing CF"
                />

                <Line
                  type="monotone"
                  dataKey="financingCashFlow"
                  stroke="#059669"
                  strokeWidth={2}
                  dot={false}
                  name="Financing CF"
                />

                <Line
                  type="monotone"
                  dataKey="closingCash"
                  stroke="#7c3aed"
                  strokeWidth={2}
                  dot={false}
                  name="Closing Cash"
                />

              </LineChart>

            </ResponsiveContainer>

          </div>

        </ChartCard>
      )}

      {/* =====================================================
          YEARLY REVENUE
          ===================================================== */}

      {yearlyRevenue.length > 0 && (
        <ChartCard
          title="Revenue Projection"
          subtitle="Projected annual revenue and customer growth"
        >

          <div className="h-[320px] w-full">

            <ResponsiveContainer
              width="100%"
              height="100%"
            >

              <BarChart
                data={yearlyRevenue}
              >

                <CartesianGrid
                  strokeDasharray="3 3"
                  stroke="#eef1f8"
                />

                <XAxis
                  dataKey="year"
                  tick={{
                    fontSize: 11,
                  }}
                />

                <YAxis
                  tick={{
                    fontSize: 11,
                  }}
                  tickFormatter={(value) =>
                    formatCompactCurrency(
                      value
                    )
                  }
                />

                <Tooltip
                  formatter={(value, name) => {

                    if (
                      name === 'totalRevenue'
                    ) {
                      return [
                        formatCurrency(
                          value
                        ),
                        'Revenue',
                      ];
                    }

                    return [
                      value,
                      name,
                    ];
                  }}
                />

                <Legend />

                <Bar
                  dataKey="totalRevenue"
                  fill="#1f2c58"
                  name="Revenue"
                />

              </BarChart>

            </ResponsiveContainer>

          </div>

        </ChartCard>
      )}

      {/* =====================================================
          BREAK-EVEN
          ===================================================== */}

      <div className="card">

        <div className="flex items-center gap-3">

          <Scale
            className="h-5 w-5 text-navy-900"
          />

          <div>

            <h3 className="text-lg font-bold text-navy-900">
              Break-even Analysis
            </h3>

            <p className="text-sm text-slate-500">
              Revenue and units required to cover
              fixed and variable costs.
            </p>

          </div>

        </div>

        <div className="mt-5 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-5">

          <StatCard
            label="Fixed Costs"
            value={formatCurrency(
              breakEven.fixedCosts
            )}
          />

          <StatCard
            label="Variable Cost / Unit"
            value={formatCurrency(
              breakEven.variableCostPerUnit
            )}
          />

          <StatCard
            label="Selling Price / Unit"
            value={formatCurrency(
              breakEven.sellingPricePerUnit
            )}
          />

          <StatCard
            label="Contribution Margin"
            value={formatCurrency(
              breakEven.contributionMargin
            )}
          />

          <StatCard
            label="Break-even Units"
            value={
              breakEven.breakEvenUnits != null
                ? formatNumber(
                    breakEven.breakEvenUnits
                  )
                : 'N/A'
            }
          />

        </div>

        <div className="mt-5 rounded-xl bg-slate-50 p-4">

          <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
            Break-even Revenue
          </p>

          <p className="mt-1 text-2xl font-extrabold text-navy-900">
            {breakEven.breakEvenRevenue != null
              ? formatCurrency(
                  breakEven.breakEvenRevenue
                )
              : 'N/A'}
          </p>

        </div>

      </div>

      {/* =====================================================
          BALANCE SHEET
          ===================================================== */}

      <div className="card">

        <div className="flex items-center gap-3">

          <Landmark
            className="h-5 w-5 text-navy-900"
          />

          <div>

            <h3 className="text-lg font-bold text-navy-900">
              Balance Sheet
            </h3>

            <p className="text-sm text-slate-500">
              Assets, liabilities and equity position — Year 1 (Month 12) snapshot.
            </p>

          </div>

        </div>

        <div className="mt-5 grid grid-cols-1 gap-6 lg:grid-cols-3">

          {/* ASSETS */}

          <BalanceSection
            title="Assets"
            items={[
              [
                'Cash',
                balanceSheet.cash,
              ],
              [
                'Accounts Receivable',
                balanceSheet.accountsReceivable,
              ],
              [
                'Inventory',
                balanceSheet.inventory,
              ],
              [
                'Net Fixed Assets',
                balanceSheet.netFixedAssets,
              ],
              [
                'Total Assets',
                balanceSheet.totalAssets,
              ],
            ]}
          />

          {/* LIABILITIES */}

          <BalanceSection
            title="Liabilities"
            items={[
              [
                'Existing Debt',
                balanceSheet.existingDebt,
              ],
              [
                'Projected Funding Gap',
                balanceSheet.projectedFundingGap,
              ],
              [
                'Accounts Payable',
                balanceSheet.accountsPayable,
              ],
              [
                'Total Liabilities',
                balanceSheet.totalLiabilities,
              ],
            ]}
          />

          {/* EQUITY */}

          <BalanceSection
            title="Equity"
            items={[
              [
                'Founder Capital',
                balanceSheet.founderCapital,
              ],
              [
                'Retained Earnings',
                balanceSheet.retainedEarnings,
              ],
              [
                'Total Equity',
                balanceSheet.totalEquity,
              ],
              [
                'Liabilities + Equity',
                balanceSheet.totalLiabilitiesAndEquity,
              ],
            ]}
          />

        </div>

        {/* ACCOUNTING CHECK */}

        <div className="mt-5 rounded-xl border border-slate-200 p-4">

          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">

            <div>

              <span className="text-sm font-semibold text-slate-600">
                Balance Check
              </span>

              <p className="mt-1 text-xs text-slate-400">
                Assets − (Liabilities + Equity)
              </p>

            </div>

            <span
              className={`font-bold ${
                isBalanced
                  ? 'text-emerald-600'
                  : 'text-brand-red'
              }`}
            >
              {formatCurrency(
                balanceCheck
              )}
            </span>

          </div>

        </div>

      </div>

      {/* =====================================================
          FINAL METRICS
          ===================================================== */}

      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">

        <StatCard
          label="ROI (Year 1)"
          value={formatPercent(
            summary.roiPercent
          )}
        />

        <StatCard
          label="Burn Rate"
          value={formatCurrency(
            summary.burnRate
          )}
        />

        <StatCard
          label="Projected Cash Runway"
          value={
            summary.runwayMonths != null
              ? `${Number(
                  summary.runwayMonths
                ).toFixed(1)} months`
              : 'Cash-flow positive'
          }
        />

        <StatCard
          label="Break-even Revenue"
          value={
            breakEven.breakEvenRevenue != null
              ? formatCurrency(
                  breakEven.breakEvenRevenue
                )
              : 'N/A'
          }
        />

      </div>

    </div>
  );
}


/* =========================================================
   METRIC CARD
   ========================================================= */

function MetricCard({
  label,
  value,
  icon: Icon,
  negative = false,
}) {
  return (
    <div className="card">

      <div className="flex items-center justify-between">

        <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
          {label}
        </p>

        {Icon && (
          <Icon
            className={`h-5 w-5 ${
              negative
                ? 'text-brand-red'
                : 'text-slate-400'
            }`}
          />
        )}

      </div>

      <p
        className={`mt-2 text-xl font-extrabold ${
          negative
            ? 'text-brand-red'
            : 'text-navy-900'
        }`}
      >
        {value}
      </p>

    </div>
  );
}


/* =========================================================
   STAT CARD
   ========================================================= */

function StatCard({
  label,
  value,
}) {
  return (
    <div className="card">

      <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
        {label}
      </p>

      <p className="mt-1 text-xl font-extrabold text-navy-900">
        {value}
      </p>

    </div>
  );
}


/* =========================================================
   BALANCE SECTION
   ========================================================= */

function BalanceSection({
  title,
  items,
}) {
  return (
    <div>

      <h4 className="mb-3 text-sm font-bold text-navy-900">
        {title}
      </h4>

      <div className="space-y-2">

        {items.map(
          ([label, value]) => (
            <div
              key={label}
              className="flex items-center justify-between border-b border-slate-100 py-2"
            >

              <span className="text-sm text-slate-500">
                {label}
              </span>

              <span className="text-sm font-semibold text-slate-800">
                {value != null
                  ? formatCurrency(value)
                  : 'N/A'}
              </span>

            </div>
          )
        )}

      </div>

    </div>
  );
}


/* =========================================================
   HEALTH LABEL
   ========================================================= */

function getHealthLabel(score) {
  if (score >= 80) {
    return 'Excellent';
  }

  if (score >= 60) {
    return 'Healthy';
  }

  if (score >= 40) {
    return 'Needs Improvement';
  }

  return 'High Risk';
}


/* =========================================================
   HEALTH ACCENT
   ========================================================= */

function getHealthAccent(score) {
  if (score >= 80) {
    return 'emerald';
  }

  if (score >= 60) {
    return 'navy';
  }

  if (score >= 40) {
    return 'amber';
  }

  return 'red';
}


/* =========================================================
   NUMBER FORMAT
   ========================================================= */

function formatNumber(value) {
  if (
    value == null ||
    Number.isNaN(Number(value))
  ) {
    return 'N/A';
  }

  return Number(value).toLocaleString(
    'en-IN',
    {
      maximumFractionDigits: 2,
    }
  );
}


/* =========================================================
   COMPACT CURRENCY
   ========================================================= */

function formatCompactCurrency(value) {
  const number =
    Number(value || 0);

  if (
    Math.abs(number) >= 10000000
  ) {
    return `₹${(
      number / 10000000
    ).toFixed(1)}Cr`;
  }

  if (
    Math.abs(number) >= 100000
  ) {
    return `₹${(
      number / 100000
    ).toFixed(1)}L`;
  }

  if (
    Math.abs(number) >= 1000
  ) {
    return `₹${(
      number / 1000
    ).toFixed(1)}K`;
  }

  return `₹${number.toFixed(0)}`;
}