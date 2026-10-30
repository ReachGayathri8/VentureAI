import React from 'react';
import { useParams } from 'react-router-dom';
import { CheckCircle, AlertTriangle, Landmark } from 'lucide-react';

import PageHeader from '../components/PageHeader';
import Loader from '../components/Loader';
import FinancialTabs from '../components/FinancialTabs';
import useFinanceData from '../hooks/useFinanceData';
import { formatCurrency } from '../utils/format';

export default function BalanceSheetPage() {
  const { startupId } = useParams();
  const { data, loading, error } = useFinanceData(startupId);

  const bs = data?.balanceSheet;

  /*
   * Calculate the accounting equation directly:
   *
   * Assets = Liabilities + Equity
   *
   * We calculate this from the displayed financial values instead
   * of relying only on the backend balanceCheck value.
   */
  const totalAssets = toNumber(bs?.totalAssets);
  const totalLiabilities = toNumber(bs?.totalLiabilities);
  const totalEquity = toNumber(bs?.totalEquity);

  const liabilitiesAndEquity =
    totalLiabilities + totalEquity;

  /*
   * Floating-point values can contain tiny decimal errors.
   * Round the calculated difference to 2 decimal places before
   * checking whether the balance sheet balances.
   */
  const rawBalanceCheck =
    totalAssets - liabilitiesAndEquity;

  const calculatedBalanceCheck =
    Math.round(
      (rawBalanceCheck + Number.EPSILON) * 100
    ) / 100;

  /*
   * A difference smaller than ₹1 is treated as balanced.
   *
   * This prevents a tiny floating-point difference from showing
   * a false red warning when the displayed value is ₹0.
   */
  const isBalanced =
    Number.isFinite(calculatedBalanceCheck) &&
    Math.abs(calculatedBalanceCheck) < 1;

  return (
    <div>
      <PageHeader
        title="Balance Sheet"
        subtitle="Assets, liabilities and equity — Assets = Liabilities + Equity."
      />

      <FinancialTabs startupId={startupId} />

      {/* ===================== LOADING ===================== */}

      {loading && (
        <Loader label="Loading balance sheet..." />
      )}

      {/* ===================== ERROR ===================== */}

      {!loading && error && (
        <div className="card text-sm text-brand-red">
          {error}
        </div>
      )}

      {/* ===================== NO DATA ===================== */}

      {!loading && !bs && !error && (
        <div className="card text-sm text-slate-500">
          No balance sheet data is available for this startup.
        </div>
      )}

      {/* ===================== BALANCE SHEET ===================== */}

      {!loading && bs && (
        <div className="space-y-6">

          {/* ===================== BALANCE STATUS ===================== */}

          <div
            className={`card border ${
              isBalanced
                ? 'border-emerald-200'
                : 'border-red-200'
            }`}
          >
            <div className="flex items-center justify-between">

              <div className="flex items-center gap-3">

                {isBalanced ? (
                  <CheckCircle className="h-6 w-6 text-emerald-600" />
                ) : (
                  <AlertTriangle className="h-6 w-6 text-brand-red" />
                )}

                <div>
                  <h3 className="font-bold text-navy-900">
                    Balance Sheet Check
                  </h3>

                  <p className="text-sm text-slate-500">
                    {isBalanced
                      ? 'The balance sheet is balanced.'
                      : 'The balance sheet does not balance.'}
                  </p>
                </div>

              </div>

              <div className="text-right">

                <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
                  Difference
                </p>

                <p
                  className={`text-xl font-extrabold ${
                    isBalanced
                      ? 'text-emerald-600'
                      : 'text-brand-red'
                  }`}
                >
                  {formatCurrency(calculatedBalanceCheck)}
                </p>

              </div>

            </div>
          </div>

          {/* ===================== ASSETS + LIABILITIES ===================== */}

          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">

            {/* ===================== ASSETS ===================== */}

            <div className="card">

              <div className="mb-4 flex items-center gap-3">
                <Landmark className="h-5 w-5 text-navy-900" />

                <h3 className="section-title">
                  Assets
                </h3>
              </div>

              <Row
                label="Cash"
                value={bs.cash}
              />

              <Row
                label="Accounts Receivable"
                value={bs.accountsReceivable}
              />

              <Row
                label="Inventory"
                value={bs.inventory}
              />

              <Row
                label="Net Fixed Assets"
                value={bs.netFixedAssets}
              />

              <Row
                label="Total Assets"
                value={bs.totalAssets}
                bold
                big
              />

            </div>

            {/* ===================== LIABILITIES ===================== */}

            <div className="card">

              <div className="mb-4 flex items-center gap-3">
                <Landmark className="h-5 w-5 text-navy-900" />

                <h3 className="section-title">
                  Liabilities
                </h3>
              </div>

              <Row
                label="Debt / Cash Shortfall"
                value={bs.debt}
              />

              <Row
                label="Accounts Payable"
                value={bs.accountsPayable}
              />

              <Row
                label="Total Liabilities"
                value={bs.totalLiabilities}
                bold
                big
              />

            </div>

          </div>

          {/* ===================== EQUITY ===================== */}

          <div className="card">

            <h3 className="section-title mb-4">
              Equity
            </h3>

            <Row
              label="Founder Capital"
              value={bs.founderCapital}
            />

            <Row
              label="Retained Earnings"
              value={bs.retainedEarnings}
            />

            <Row
              label="Total Equity"
              value={bs.totalEquity}
              bold
              big
            />

          </div>

          {/* ===================== ACCOUNTING EQUATION ===================== */}

          <div className="card">

            <h3 className="section-title mb-4">
              Accounting Equation
            </h3>

            <div className="grid grid-cols-1 gap-4 md:grid-cols-3">

              <EquationCard
                label="Total Assets"
                value={bs.totalAssets}
              />

              <EquationCard
                label="Total Liabilities"
                value={bs.totalLiabilities}
              />

              <EquationCard
                label="Total Equity"
                value={bs.totalEquity}
              />

            </div>

            {/* ===================== LIABILITIES + EQUITY ===================== */}

            <div className="mt-5 rounded-xl bg-slate-50 p-4">

              <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">

                <div>

                  <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
                    Liabilities + Equity
                  </p>

                  <p className="mt-1 text-2xl font-extrabold text-navy-900">
                    {formatCurrency(liabilitiesAndEquity)}
                  </p>

                </div>

                <div className="text-left sm:text-right">

                  <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
                    Balance Check
                  </p>

                  <p
                    className={`mt-1 text-xl font-extrabold ${
                      isBalanced
                        ? 'text-emerald-600'
                        : 'text-brand-red'
                    }`}
                  >
                    {formatCurrency(calculatedBalanceCheck)}
                  </p>

                </div>

              </div>

            </div>

          </div>

        </div>
      )}
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


/* =========================================================
   BALANCE SHEET ROW
   ========================================================= */

function Row({
  label,
  value,
  bold = false,
  big = false,
}) {
  return (
    <div
      className={`flex items-center justify-between border-b border-slate-50 py-3 ${
        bold
          ? 'border-t border-slate-100'
          : ''
      }`}
    >

      <span
        className={
          bold
            ? 'font-bold text-navy-900'
            : 'text-slate-600'
        }
      >
        {label}
      </span>

      <span
        className={`${
          bold
            ? 'font-extrabold text-navy-900'
            : 'font-medium text-slate-700'
        } ${big ? 'text-lg' : ''}`}
      >
        {value !== null && value !== undefined
          ? formatCurrency(value)
          : 'N/A'}
      </span>

    </div>
  );
}


/* =========================================================
   ACCOUNTING EQUATION CARD
   ========================================================= */

function EquationCard({
  label,
  value,
}) {
  return (
    <div className="rounded-xl bg-slate-50 p-4">

      <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
        {label}
      </p>

      <p className="mt-1 text-xl font-extrabold text-navy-900">
        {value !== null && value !== undefined
          ? formatCurrency(value)
          : 'N/A'}
      </p>

    </div>
  );
}
