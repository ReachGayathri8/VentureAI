import React, { useState } from 'react';
import { useParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  Legend,
} from 'recharts';
import { RefreshCw, TrendingUp } from 'lucide-react';

import PageHeader from '../components/PageHeader';
import Loader from '../components/Loader';
import FinancialTabs from '../components/FinancialTabs';
import ChartCard from '../charts/ChartCard';
import useFinanceData from '../hooks/useFinanceData';
import { financeApi } from '../api/finance';
import { formatCurrency } from '../utils/format';

export default function RevenueProjectionPage() {
  const { startupId } = useParams();

  const {
    data,
    loading,
    error,
    reload,
  } = useFinanceData(startupId);

  const [assumptions, setAssumptions] = useState(null);
  const [recalculating, setRecalculating] = useState(false);
  const [preview, setPreview] = useState(null);

  /*
   * Use recalculated preview when available.
   * Otherwise use the saved projection.
   */
  const projection =
    preview?.revenueProjectionYearly ||
    data?.revenueProjectionYearly ||
    [];

  /*
   * Start editing using Year 1's average monthly
   * customer count.
   *
   * The backend calculates "customers" in the yearly
   * projection as the average monthly customer base.
   */
  const startEditing = () => {
    const yearly =
      data?.revenueProjectionYearly?.[0];

    setAssumptions({
      expectedMonthlyCustomers:
        yearly?.customers ?? 0,

      averageCustomerRevenue:
        yearly?.averageRevenuePerCustomer ?? 0,

      expectedGrowthRate:
        yearly?.growthRate ?? 0,

      projectionPeriodYears:
        data?.revenueProjectionYearly?.length || 3,
    });
  };

  /*
   * Recalculate a what-if projection.
   */
  const recalculate = async () => {
    if (!assumptions) return;

    /*
     * Basic frontend validation.
     */
    if (
      assumptions.expectedMonthlyCustomers < 0 ||
      assumptions.averageCustomerRevenue < 0 ||
      assumptions.projectionPeriodYears < 1
    ) {
      toast.error(
        'Please enter valid non-negative financial assumptions.'
      );
      return;
    }

    setRecalculating(true);

    try {
      const result =
        await financeApi.recalculate(
          startupId,
          assumptions
        );

      setPreview(result);

      toast.success(
        'Projection recalculated.'
      );
    } catch (err) {
      console.error(err);

      toast.error(
        'Unable to recalculate. Check your inputs.'
      );
    } finally {
      setRecalculating(false);
    }
  };

  /*
   * Reset to the saved backend projection.
   */
  const resetProjection = () => {
    setPreview(null);
    setAssumptions(null);
    reload();
  };

  return (
    <div>

      <PageHeader
        title="Revenue Projection"
        subtitle="Projected revenue based on customer volume, revenue per customer and annual growth assumptions."
      />

      <FinancialTabs startupId={startupId} />

      {/* ===================== LOADING ===================== */}

      {loading && (
        <Loader
          label="Loading revenue projection..."
        />
      )}

      {/* ===================== ERROR ===================== */}

      {!loading && error && (
        <div className="card text-sm text-brand-red">
          {error}
        </div>
      )}

      {/* ===================== NO DATA ===================== */}

      {!loading &&
        !error &&
        projection.length === 0 && (
          <div className="card text-sm text-slate-500">
            No revenue projection data is available
            for this startup.
          </div>
        )}

      {/* ===================== PROJECTION ===================== */}

      {!loading &&
        !error &&
        projection.length > 0 && (
          <>

            {/* ===================== SUMMARY ===================== */}

            <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">

              <SummaryCard
                label="Year 1 Revenue"
                value={formatCurrency(
                  projection[0]?.totalRevenue
                )}
              />

              <SummaryCard
                label={`Year ${projection.length} Revenue`}
                value={formatCurrency(
                  projection[
                    projection.length - 1
                  ]?.totalRevenue
                )}
              />

              <SummaryCard
                label="Projection Period"
                value={`${projection.length} years`}
              />

            </div>

            {/* ===================== CHART ===================== */}

            <ChartCard
              title="Revenue Projection by Year"
              subtitle="Projected total revenue for each year"
            >

              <BarChart
                data={projection}
                margin={{
                  top: 10,
                  right: 20,
                  left: 10,
                  bottom: 10,
                }}
              >

                <CartesianGrid
                  strokeDasharray="3 3"
                  stroke="#eef1f8"
                />

                <XAxis
                  dataKey="year"
                  tickFormatter={(year) =>
                    `Year ${year}`
                  }
                  tick={{
                    fontSize: 12,
                  }}
                />

                <YAxis
                  tick={{
                    fontSize: 12,
                  }}
                />

                <Tooltip
                  formatter={(value) =>
                    formatCurrency(value)
                  }
                  labelFormatter={(year) =>
                    `Year ${year}`
                  }
                />

                <Legend />

                <Bar
                  dataKey="totalRevenue"
                  fill="#1f2c58"
                  radius={[
                    6,
                    6,
                    0,
                    0,
                  ]}
                  name="Total Revenue"
                />

              </BarChart>

            </ChartCard>

            {/* ===================== YEARLY BREAKDOWN ===================== */}

            <div className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-3">

              <div className="card overflow-x-auto lg:col-span-2">

                <div className="mb-4 flex items-center gap-2">
                  <TrendingUp
                    size={18}
                    className="text-navy-900"
                  />

                  <h3 className="section-title">
                    Yearly Breakdown
                  </h3>
                </div>

                <table className="w-full min-w-[800px] text-left text-sm">

                  <thead>
                    <tr className="border-b border-slate-100 text-xs uppercase tracking-wide text-slate-400">

                      <th className="pb-3 pr-4">
                        Year
                      </th>

                      <th className="pb-3 pr-4">
                        Avg. Monthly Customers
                      </th>

                      <th className="pb-3 pr-4">
                        Revenue / Customer
                      </th>

                      <th className="pb-3 pr-4">
                        Total Revenue
                      </th>

                      <th className="pb-3">
                        Annual Growth
                      </th>

                    </tr>
                  </thead>

                  <tbody>

                    {projection.map((row) => (

                      <tr
                        key={row.year}
                        className="border-b border-slate-50 last:border-0"
                      >

                        <td className="py-3 pr-4 font-semibold text-navy-900">
                          Year {row.year}
                        </td>

                        <td className="py-3 pr-4">
                          {toNumber(
                            row.customers
                          ).toLocaleString(
                            'en-IN'
                          )}
                        </td>

                        <td className="py-3 pr-4">
                          {formatCurrency(
                            row.averageRevenuePerCustomer
                          )}
                        </td>

                        <td className="py-3 pr-4 font-semibold text-navy-900">
                          {formatCurrency(
                            row.totalRevenue
                          )}
                        </td>

                        <td className="py-3">
                          {formatGrowthRate(
                            row.growthRate
                          )}
                        </td>

                      </tr>

                    ))}

                  </tbody>

                </table>

              </div>

              {/* ===================== ASSUMPTIONS ===================== */}

              <div className="card">

                <h3 className="section-title mb-2">
                  Adjust Assumptions
                </h3>

                <p className="mb-4 text-xs leading-5 text-slate-400">
                  Change the assumptions to create a
                  what-if revenue projection.
                </p>

                {!assumptions ? (

                  <button
                    onClick={startEditing}
                    className="btn-outline w-full"
                  >
                    Edit Assumptions
                  </button>

                ) : (

                  <div className="space-y-4">

                    <NumberInput
                      label="Starting Monthly Customers"
                      value={
                        assumptions.expectedMonthlyCustomers
                      }
                      min={0}
                      step={1}
                      onChange={(value) =>
                        setAssumptions((current) => ({
                          ...current,
                          expectedMonthlyCustomers:
                            value,
                        }))
                      }
                    />

                    <NumberInput
                      label="Average Revenue / Customer"
                      value={
                        assumptions.averageCustomerRevenue
                      }
                      min={0}
                      step={100}
                      onChange={(value) =>
                        setAssumptions((current) => ({
                          ...current,
                          averageCustomerRevenue:
                            value,
                        }))
                      }
                    />

                    <NumberInput
                      label="Annual Growth Rate (%)"
                      value={
                        assumptions.expectedGrowthRate
                      }
                      min={-99}
                      step={1}
                      onChange={(value) =>
                        setAssumptions((current) => ({
                          ...current,
                          expectedGrowthRate:
                            value,
                        }))
                      }
                    />

                    <NumberInput
                      label="Projection Years"
                      value={
                        assumptions.projectionPeriodYears
                      }
                      min={1}
                      max={10}
                      step={1}
                      onChange={(value) =>
                        setAssumptions((current) => ({
                          ...current,
                          projectionPeriodYears:
                            value,
                        }))
                      }
                    />

                    <button
                      onClick={recalculate}
                      disabled={recalculating}
                      className="btn-accent flex w-full items-center justify-center gap-2"
                    >

                      <RefreshCw
                        size={15}
                        className={
                          recalculating
                            ? 'animate-spin'
                            : ''
                        }
                      />

                      {recalculating
                        ? 'Recalculating...'
                        : 'Recalculate'}

                    </button>

                    {preview && (
                      <div className="rounded-xl bg-emerald-50 p-3">

                        <p className="text-xs font-semibold text-emerald-700">
                          Preview Mode
                        </p>

                        <p className="mt-1 text-xs leading-5 text-emerald-600">
                          These figures are a what-if
                          projection and are not the
                          saved startup assumptions.
                        </p>

                      </div>
                    )}

                    <button
                      onClick={resetProjection}
                      className="btn-outline w-full text-xs"
                    >
                      Reset to Saved Figures
                    </button>

                  </div>

                )}

              </div>

            </div>

            {/* ===================== METHODOLOGY ===================== */}

            <div className="mt-6 card">

              <h3 className="section-title mb-3">
                Projection Method
              </h3>

              <div className="grid grid-cols-1 gap-4 md:grid-cols-3">

                <MethodCard
                  title="Customer Base"
                  text="The projection starts with the expected monthly customer base and applies the annual growth assumption using monthly compounding."
                />

                <MethodCard
                  title="Revenue"
                  text="Monthly revenue is calculated from projected customers multiplied by revenue per customer."
                />

                <MethodCard
                  title="Annual Revenue"
                  text="Each year's revenue is the sum of the twelve projected monthly revenue values."
                />

              </div>

            </div>

          </>
        )}

    </div>
  );
}


/* =========================================================
   SUMMARY CARD
   ========================================================= */

function SummaryCard({
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
   NUMBER INPUT
   ========================================================= */

function NumberInput({
  label,
  value,
  onChange,
  min,
  max,
  step,
}) {
  return (
    <div>

      <label className="label-field text-xs">
        {label}
      </label>

      <input
        type="number"
        className="input-field"
        value={value ?? ''}
        min={min}
        max={max}
        step={step}
        onChange={(event) => {
          const raw = event.target.value;

          if (raw === '') {
            onChange(0);
            return;
          }

          const parsed =
            Number(raw);

          onChange(
            Number.isFinite(parsed)
              ? parsed
              : 0
          );
        }}
      />

    </div>
  );
}


/* =========================================================
   METHOD CARD
   ========================================================= */

function MethodCard({
  title,
  text,
}) {
  return (
    <div className="rounded-xl bg-slate-50 p-4">

      <p className="text-sm font-bold text-navy-900">
        {title}
      </p>

      <p className="mt-2 text-sm leading-6 text-slate-500">
        {text}
      </p>

    </div>
  );
}


/* =========================================================
   SAFE NUMBER
   ========================================================= */

function toNumber(value) {
  if (
    value === null ||
    value === undefined ||
    value === ''
  ) {
    return 0;
  }

  const number =
    Number(value);

  return Number.isFinite(number)
    ? number
    : 0;
}


/* =========================================================
   GROWTH FORMATTER
   ========================================================= */

function formatGrowthRate(value) {
  const number =
    toNumber(value);

  return `${number.toFixed(1)}%`;
}
