import React, { useState } from 'react';
import { useParams } from 'react-router-dom';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  ResponsiveContainer,
  Legend,
  LineChart,
  Line,
} from 'recharts';
import PageHeader from '../components/PageHeader';
import Loader from '../components/Loader';
import FinancialTabs from '../components/FinancialTabs';
import ChartCard from '../charts/ChartCard';
import useFinanceData from '../hooks/useFinanceData';
import { formatCurrency } from '../utils/format';

export default function CashFlowPage() {
  const { startupId } = useParams();
  const { data, loading, error } = useFinanceData(startupId);
  const [yearFilter, setYearFilter] = useState('all');

  const allRows = data?.cashFlowMonthly || [];

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
        title="Cash Flow Statement"
        subtitle="Operating, investing and financing cash flows with closing cash."
      />

      <FinancialTabs startupId={startupId} />

      {loading && (
        <Loader label="Loading cash flow..." />
      )}

      {!loading && error && (
        <div className="card text-sm text-brand-red">
          {error}
        </div>
      )}

      {!loading && data && (
        <>
          {/* CASH BALANCE CHART */}
          {rows.length > 0 && (
            <ChartCard
              title="Cash Balance Over Time"
              subtitle="Closing cash balance month by month"
            >
              <div className="h-[340px] w-full">
                <ResponsiveContainer
                  width="100%"
                  height="100%"
                >
                  <AreaChart data={rows}>
                    <defs>
                      <linearGradient
                        id="cashGradient"
                        x1="0"
                        y1="0"
                        x2="0"
                        y2="1"
                      >
                        <stop
                          offset="5%"
                          stopColor="#1f2c58"
                          stopOpacity={0.4}
                        />

                        <stop
                          offset="95%"
                          stopColor="#1f2c58"
                          stopOpacity={0}
                        />
                      </linearGradient>
                    </defs>

                    <CartesianGrid
                      strokeDasharray="3 3"
                      stroke="#eef1f8"
                    />

                    <XAxis
                      dataKey="label"
                      tick={{ fontSize: 11 }}
                      interval={2}
                    />

                    <YAxis
                      tick={{ fontSize: 11 }}
                      tickFormatter={(value) =>
                        formatCompactCurrency(value)
                      }
                    />

                    <Tooltip
                      formatter={(value) =>
                        formatCurrency(value)
                      }
                    />

                    <Legend />

                    <Area
                      type="monotone"
                      dataKey="closingCash"
                      stroke="#1f2c58"
                      fill="url(#cashGradient)"
                      strokeWidth={2}
                      name="Closing Cash"
                    />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </ChartCard>
          )}

          {/* CASH FLOW COMPONENTS */}
          {rows.length > 0 && (
            <div className="mt-6">
              <ChartCard
                title="Cash Flow Components"
                subtitle="Operating, investing and financing activity"
              >
                <div className="h-[340px] w-full">
                  <ResponsiveContainer
                    width="100%"
                    height="100%"
                  >
                    <LineChart data={rows}>
                      <CartesianGrid
                        strokeDasharray="3 3"
                        stroke="#eef1f8"
                      />

                      <XAxis
                        dataKey="label"
                        tick={{ fontSize: 11 }}
                        interval={2}
                      />

                      <YAxis
                        tick={{ fontSize: 11 }}
                        tickFormatter={(value) =>
                          formatCompactCurrency(value)
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
                        dataKey="netCashFlow"
                        stroke="#7c3aed"
                        strokeWidth={2}
                        dot={false}
                        name="Net Cash Flow"
                      />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              </ChartCard>
            </div>
          )}

          {/* MONTHLY CASH FLOW TABLE */}
          <div className="mt-6 card overflow-x-auto">

            <div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">

              <div>
                <h3 className="section-title">
                  Monthly Cash Flow
                </h3>

                <p className="mt-1 text-xs text-slate-400">
                  Cash movement across operating, investing and financing activities.
                </p>
              </div>

              {totalYears > 1 && (
                <select
                  className="input-field w-40"
                  value={yearFilter}
                  onChange={(e) =>
                    setYearFilter(e.target.value)
                  }
                >
                  <option value="all">
                    All Years
                  </option>

                  {Array.from({
                    length: totalYears,
                  }).map((_, i) => (
                    <option
                      key={i}
                      value={i + 1}
                    >
                      Year {i + 1}
                    </option>
                  ))}
                </select>
              )}

            </div>

            <table className="w-full min-w-[1100px] text-left text-sm">

              <thead>
                <tr className="border-b border-slate-100 text-xs uppercase tracking-wide text-slate-400">

                  <th className="pb-3 pr-5">
                    Month
                  </th>

                  <th className="pb-3 pr-5">
                    Opening Cash
                  </th>

                  <th className="pb-3 pr-5">
                    Operating CF
                  </th>

                  <th className="pb-3 pr-5">
                    Investing CF
                  </th>

                  <th className="pb-3 pr-5">
                    Financing CF
                  </th>

                  <th className="pb-3 pr-5">
                    Net Cash Flow
                  </th>

                  <th className="pb-3">
                    Closing Cash
                  </th>

                </tr>
              </thead>

              <tbody>

                {rows.length === 0 && (
                  <tr>
                    <td
                      colSpan="7"
                      className="py-8 text-center text-sm text-slate-400"
                    >
                      No cash flow data available.
                    </td>
                  </tr>
                )}

                {rows.map((r) => (
                  <tr
                    key={r.period}
                    className="border-b border-slate-50 last:border-0 hover:bg-slate-50"
                  >

                    <td className="py-3 pr-5 font-semibold text-navy-900">
                      {r.label}
                    </td>

                    <td className="py-3 pr-5">
                      {formatCurrency(r.openingCash)}
                    </td>

                    <td
                      className={`py-3 pr-5 font-medium ${
                        Number(r.operatingCashFlow) >= 0
                          ? 'text-emerald-600'
                          : 'text-brand-red'
                      }`}
                    >
                      {formatCurrency(
                        r.operatingCashFlow
                      )}
                    </td>

                    <td
                      className={`py-3 pr-5 font-medium ${
                        Number(r.investingCashFlow) >= 0
                          ? 'text-emerald-600'
                          : 'text-brand-red'
                      }`}
                    >
                      {formatCurrency(
                        r.investingCashFlow
                      )}
                    </td>

                    <td
                      className={`py-3 pr-5 font-medium ${
                        Number(r.financingCashFlow) >= 0
                          ? 'text-emerald-600'
                          : 'text-brand-red'
                      }`}
                    >
                      {formatCurrency(
                        r.financingCashFlow
                      )}
                    </td>

                    <td
                      className={`py-3 pr-5 font-bold ${
                        Number(r.netCashFlow) >= 0
                          ? 'text-emerald-600'
                          : 'text-brand-red'
                      }`}
                    >
                      {formatCurrency(
                        r.netCashFlow
                      )}
                    </td>

                    <td
                      className={`py-3 font-bold ${
                        Number(r.closingCash) >= 0
                          ? 'text-navy-900'
                          : 'text-brand-red'
                      }`}
                    >
                      {formatCurrency(
                        r.closingCash
                      )}
                    </td>

                  </tr>
                ))}

              </tbody>

            </table>

            {/* CALCULATION NOTE */}
            <div className="mt-5 rounded-xl bg-slate-50 p-4">

              <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
                Cash Flow Calculation
              </p>

              <p className="mt-2 text-sm text-slate-600">
                Net Cash Flow = Operating CF + Investing CF + Financing CF
              </p>

              <p className="text-sm text-slate-600">
                Closing Cash = Opening Cash + Net Cash Flow
              </p>

            </div>

          </div>
        </>
      )}
    </div>
  );
}

function formatCompactCurrency(value) {
  const number = Number(value || 0);

  if (Math.abs(number) >= 10000000) {
    return `₹${(number / 10000000).toFixed(1)}Cr`;
  }

  if (Math.abs(number) >= 100000) {
    return `₹${(number / 100000).toFixed(1)}L`;
  }

  if (Math.abs(number) >= 1000) {
    return `₹${(number / 1000).toFixed(1)}K`;
  }

  return `₹${number.toFixed(0)}`;
}
