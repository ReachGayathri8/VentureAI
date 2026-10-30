import React, { useState } from 'react';
import { useParams } from 'react-router-dom';
import PageHeader from '../components/PageHeader';
import Loader from '../components/Loader';
import FinancialTabs from '../components/FinancialTabs';
import useFinanceData from '../hooks/useFinanceData';
import { formatCurrency } from '../utils/format';

export default function PnLPage() {
  const { startupId } = useParams();
  const { data, loading, error } = useFinanceData(startupId);
  const [yearFilter, setYearFilter] = useState('all');

  const allRows = data?.pnlMonthly || [];

  const rows = allRows.filter((r) => {
    if (yearFilter === 'all') {
      return true;
    }

    const year = Math.ceil(Number(r.period) / 12);
    return year === Number(yearFilter);
  });

  const totalYears = Math.ceil(allRows.length / 12);

  return (
    <div>
      <PageHeader
        title="Profit & Loss Statement"
        subtitle="Detailed monthly revenue, costs, profitability and tax analysis."
      />

      <FinancialTabs startupId={startupId} />

      {loading && <Loader label="Loading P&L..." />}

      {!loading && error && (
        <div className="card text-sm text-brand-red">
          {error}
        </div>
      )}

      {!loading && data && (
        <div className="card overflow-x-auto">

          {/* HEADER */}
          <div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">

            <div>
              <h3 className="section-title">
                Monthly P&amp;L ({allRows.length}-month projection)
              </h3>

              <p className="mt-1 text-xs text-slate-400">
                Revenue → COGS → Gross Profit → Operating Expenses →
                Depreciation → EBIT → Interest → Tax → Net Profit
              </p>
            </div>

            {totalYears > 1 && (
              <select
                className="input-field w-40"
                value={yearFilter}
                onChange={(e) => setYearFilter(e.target.value)}
              >
                <option value="all">All Years</option>

                {Array.from({ length: totalYears }).map((_, i) => (
                  <option key={i} value={i + 1}>
                    Year {i + 1}
                  </option>
                ))}
              </select>
            )}

          </div>

          {/* TABLE */}
          <table className="w-full min-w-[1250px] text-left text-sm">

            <thead>
              <tr className="border-b border-slate-100 text-xs uppercase tracking-wide text-slate-400">

                <th className="pb-3 pr-5">
                  Month
                </th>

                <th className="pb-3 pr-5">
                  Revenue
                </th>

                <th className="pb-3 pr-5">
                  COGS
                </th>

                <th className="pb-3 pr-5">
                  Gross Profit
                </th>

                <th className="pb-3 pr-5">
                  Operating Expenses
                </th>

                <th className="pb-3 pr-5">
                  Depreciation
                </th>

                <th className="pb-3 pr-5">
                  EBIT
                </th>

                <th className="pb-3 pr-5">
                  Interest
                </th>

                <th className="pb-3 pr-5">
                  Profit Before Tax
                </th>

                <th className="pb-3 pr-5">
                  Tax
                </th>

                <th className="pb-3">
                  Net Profit
                </th>

              </tr>
            </thead>

            <tbody>

              {rows.length === 0 && (
                <tr>
                  <td
                    colSpan="11"
                    className="py-8 text-center text-sm text-slate-400"
                  >
                    No P&amp;L data available.
                  </td>
                </tr>
              )}

              {rows.map((r) => (
                <tr
                  key={r.period}
                  className="border-b border-slate-50 last:border-0 hover:bg-slate-50"
                >

                  {/* MONTH */}
                  <td className="py-3 pr-5 font-semibold text-navy-900">
                    {r.label}
                  </td>

                  {/* REVENUE */}
                  <td className="py-3 pr-5 font-medium text-slate-700">
                    {formatCurrency(r.revenue)}
                  </td>

                  {/* COGS */}
                  <td className="py-3 pr-5 text-slate-600">
                    {formatCurrency(r.cogs)}
                  </td>

                  {/* GROSS PROFIT */}
                  <td
                    className={`py-3 pr-5 font-semibold ${
                      Number(r.grossProfit) >= 0
                        ? 'text-emerald-600'
                        : 'text-brand-red'
                    }`}
                  >
                    {formatCurrency(r.grossProfit)}
                  </td>

                  {/* OPERATING EXPENSES */}
                  <td className="py-3 pr-5 text-slate-600">
                    {formatCurrency(r.operatingExpenses)}
                  </td>

                  {/* DEPRECIATION */}
                  <td className="py-3 pr-5 text-slate-600">
                    {formatCurrency(r.depreciation)}
                  </td>

                  {/* EBIT */}
                  <td
                    className={`py-3 pr-5 font-semibold ${
                      Number(r.ebit) >= 0
                        ? 'text-emerald-600'
                        : 'text-brand-red'
                    }`}
                  >
                    {formatCurrency(r.ebit)}
                  </td>

                  {/* INTEREST */}
                  <td className="py-3 pr-5 text-slate-600">
                    {formatCurrency(r.interestExpense)}
                  </td>

                  {/* PROFIT BEFORE TAX */}
                  <td
                    className={`py-3 pr-5 font-semibold ${
                      Number(r.profitBeforeTax) >= 0
                        ? 'text-emerald-600'
                        : 'text-brand-red'
                    }`}
                  >
                    {formatCurrency(r.profitBeforeTax)}
                  </td>

                  {/* TAX */}
                  <td className="py-3 pr-5 text-slate-600">
                    {formatCurrency(r.tax)}
                  </td>

                  {/* NET PROFIT */}
                  <td
                    className={`py-3 font-bold ${
                      Number(r.netProfit) >= 0
                        ? 'text-emerald-600'
                        : 'text-brand-red'
                    }`}
                  >
                    {formatCurrency(r.netProfit)}
                  </td>

                </tr>
              ))}

            </tbody>

          </table>

          {/* CALCULATION NOTE */}
          <div className="mt-5 rounded-xl bg-slate-50 p-4">

            <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
              P&amp;L Calculation
            </p>

            <p className="mt-2 text-sm text-slate-600">
              Gross Profit = Revenue − COGS
            </p>

            <p className="text-sm text-slate-600">
              EBIT = Gross Profit − Operating Expenses − Depreciation
            </p>

            <p className="text-sm text-slate-600">
              Profit Before Tax = EBIT − Interest Expense
            </p>

            <p className="text-sm text-slate-600">
              Net Profit = Profit Before Tax − Tax
            </p>

          </div>

        </div>
      )}
    </div>
  );
}
