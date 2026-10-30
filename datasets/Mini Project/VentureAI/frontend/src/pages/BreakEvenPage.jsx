import React, { useMemo } from 'react';
import { useParams } from 'react-router-dom';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  ReferenceDot,
  Legend,
} from 'recharts';

import PageHeader from '../components/PageHeader';
import Loader from '../components/Loader';
import FinancialTabs from '../components/FinancialTabs';
import ChartCard from '../charts/ChartCard';
import useFinanceData from '../hooks/useFinanceData';
import { formatCurrency } from '../utils/format';

export default function BreakEvenPage() {
  const { startupId } = useParams();

  const {
    data,
    loading,
    error,
  } = useFinanceData(startupId);

  const be = data?.breakEven;

  /*
   * Convert API values safely to numbers.
   */
  const fixedCosts =
    toNumber(be?.fixedCosts);

  const variableCostPerUnit =
    toNumber(be?.variableCostPerUnit);

  const sellingPricePerUnit =
    toNumber(be?.sellingPricePerUnit);

  const contributionMargin =
    toNumber(be?.contributionMargin);

  const breakEvenUnits =
    toNumber(be?.breakEvenUnits);

  const breakEvenRevenue =
    toNumber(be?.breakEvenRevenue);

  /*
   * Break-even is valid only when:
   *
   * Selling Price > Variable Cost
   *
   * Contribution Margin must therefore be positive.
   */
  const canCalculateBreakEven =
    fixedCosts > 0 &&
    sellingPricePerUnit > variableCostPerUnit &&
    contributionMargin > 0 &&
    breakEvenUnits > 0;

  /*
   * Build chart data.
   *
   * Total Cost =
   * Fixed Costs + Variable Cost × Units
   *
   * Total Revenue =
   * Selling Price × Units
   */
  const chartData = useMemo(() => {
    if (!canCalculateBreakEven) {
      return [];
    }

    const chartBreakEvenUnits =
      Math.ceil(breakEvenUnits);

    /*
     * Show approximately twice the break-even volume
     * so the intersection is clearly visible.
     */
    const maxUnits =
      Math.max(
        chartBreakEvenUnits * 2,
        10
      );

    /*
     * Create around 10–12 points.
     */
    const step =
      Math.max(
        1,
        Math.ceil(maxUnits / 10)
      );

    const points = [];

    for (
      let units = 0;
      units <= maxUnits;
      units += step
    ) {
      points.push({
        units,
        totalCost:
          fixedCosts
          + variableCostPerUnit * units,

        totalRevenue:
          sellingPricePerUnit * units,
      });
    }

    /*
     * Add the exact break-even point so the
     * chart contains the actual intersection.
     */
    points.push({
      units: breakEvenUnits,
      totalCost: breakEvenRevenue,
      totalRevenue: breakEvenRevenue,
    });

    /*
     * Sort and remove duplicate unit values.
     */
    return points
      .sort((a, b) => a.units - b.units)
      .filter(
        (point, index, array) =>
          index === 0 ||
          point.units !== array[index - 1].units
      );
  }, [
    canCalculateBreakEven,
    breakEvenUnits,
    breakEvenRevenue,
    fixedCosts,
    variableCostPerUnit,
    sellingPricePerUnit,
  ]);

  return (
    <div>

      <PageHeader
        title="Break-even Analysis"
        subtitle="The point where total revenue equals total cost."
      />

      <FinancialTabs startupId={startupId} />

      {/* ===================== LOADING ===================== */}

      {loading && (
        <Loader label="Loading break-even analysis..." />
      )}

      {/* ===================== ERROR ===================== */}

      {!loading && error && (
        <div className="card text-sm text-brand-red">
          {error}
        </div>
      )}

      {/* ===================== DATA ===================== */}

      {!loading && !error && be && (
        <>
          {/* ===================== BASIC METRICS ===================== */}

          <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">

            <Stat
              label="Fixed Costs"
              value={formatCurrency(fixedCosts)}
            />

            <Stat
              label="Variable Cost / Unit"
              value={formatCurrency(variableCostPerUnit)}
            />

            <Stat
              label="Selling Price / Unit"
              value={formatCurrency(sellingPricePerUnit)}
            />

            <Stat
              label="Contribution Margin"
              value={formatCurrency(contributionMargin)}
            />

          </div>

          {/* ===================== INVALID BREAK-EVEN ===================== */}

          {!canCalculateBreakEven ? (
            <div className="card">

              <div className="flex items-start gap-3">

                <div className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-amber-50 text-amber-600">
                  !
                </div>

                <div>

                  <h3 className="font-bold text-navy-900">
                    Break-even point unavailable
                  </h3>

                  <p className="mt-1 text-sm leading-6 text-slate-500">
                    Break-even requires fixed costs greater than zero
                    and a positive contribution margin.
                  </p>

                  <p className="mt-2 text-sm leading-6 text-slate-500">
                    The selling price per unit must be greater than
                    the variable cost per unit.
                  </p>

                </div>

              </div>

            </div>
          ) : (
            <>
              {/* ===================== BREAK-EVEN RESULTS ===================== */}

              <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2">

                <Stat
                  label="Break-even Units"
                  value={Math.ceil(
                    breakEvenUnits
                  ).toLocaleString('en-IN')}
                  accent
                />

                <Stat
                  label="Break-even Revenue"
                  value={formatCurrency(
                    breakEvenRevenue
                  )}
                  accent
                />

              </div>

              {/* ===================== FORMULA ===================== */}

              <div className="card mb-6">

                <h3 className="section-title mb-3">
                  Break-even Formula
                </h3>

                <div className="rounded-xl bg-slate-50 p-4">

                  <p className="text-sm text-slate-600">
                    Break-even Units =
                  </p>

                  <p className="mt-1 text-lg font-bold text-navy-900">
                    Fixed Costs ÷ Contribution Margin
                  </p>

                  <p className="mt-2 text-sm text-slate-500">
                    {formatCurrency(fixedCosts)}
                    {' ÷ '}
                    {formatCurrency(contributionMargin)}
                    {' = '}
                    {Math.ceil(
                      breakEvenUnits
                    ).toLocaleString('en-IN')}
                    {' units'}
                  </p>

                </div>

              </div>

              {/* ===================== CHART ===================== */}

              <ChartCard
                title="Break-even Chart"
                subtitle="Total Cost vs Total Revenue by unit volume"
              >

                <LineChart
                  data={chartData}
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
                    dataKey="units"
                    tick={{ fontSize: 11 }}
                    label={{
                      value: 'Units Sold',
                      position: 'insideBottom',
                      offset: -5,
                      fontSize: 11,
                    }}
                  />

                  <YAxis
                    tick={{ fontSize: 11 }}
                  />

                  <Tooltip
                    formatter={(value) =>
                      formatCurrency(value)
                    }
                  />

                  <Legend />

                  <Line
                    type="monotone"
                    dataKey="totalCost"
                    stroke="#c81e2d"
                    strokeWidth={2}
                    dot={false}
                    name="Total Cost"
                  />

                  <Line
                    type="monotone"
                    dataKey="totalRevenue"
                    stroke="#1f2c58"
                    strokeWidth={2}
                    dot={false}
                    name="Total Revenue"
                  />

                  <ReferenceDot
                    x={breakEvenUnits}
                    y={breakEvenRevenue}
                    r={7}
                    fill="#059669"
                    stroke="white"
                    strokeWidth={2}
                  />

                </LineChart>

              </ChartCard>

              {/* ===================== INTERPRETATION ===================== */}

              <div className="mt-6 card">

                <h3 className="section-title mb-3">
                  Interpretation
                </h3>

                <div className="grid grid-cols-1 gap-4 md:grid-cols-3">

                  <InfoBox
                    label="Below Break-even"
                    text="The business operates at a loss because total costs exceed total revenue."
                  />

                  <InfoBox
                    label="At Break-even"
                    text="Total revenue equals total cost. The business has neither profit nor loss."
                  />

                  <InfoBox
                    label="Above Break-even"
                    text="Each additional unit contributes toward operating profit after variable costs."
                  />

                </div>

              </div>

            </>
          )}
        </>
      )}

      {/* ===================== NO DATA ===================== */}

      {!loading && !error && !be && (
        <div className="card text-sm text-slate-500">
          No break-even data is available for this startup.
        </div>
      )}

    </div>
  );
}


/* =========================================================
   STAT CARD
   ========================================================= */

function Stat({
  label,
  value,
  accent = false,
}) {
  return (
    <div
      className={`card ${
        accent
          ? 'border-navy-200 bg-navy-50'
          : ''
      }`}
    >

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
   INFORMATION BOX
   ========================================================= */

function InfoBox({
  label,
  text,
}) {
  return (
    <div className="rounded-xl bg-slate-50 p-4">

      <p className="text-sm font-bold text-navy-900">
        {label}
      </p>

      <p className="mt-2 text-sm leading-6 text-slate-500">
        {text}
      </p>

    </div>
  );
}


/* =========================================================
   SAFE NUMBER CONVERSION
   ========================================================= */

function toNumber(value) {
  if (
    value === null ||
    value === undefined ||
    value === ''
  ) {
    return 0;
  }

  const number = Number(value);

  return Number.isFinite(number)
    ? number
    : 0;
}
