import React, { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import {
  Eye,
  Download,
  GitCompare,
  History,
  Search,
  Trophy,
  BarChart3,
  X,
} from 'lucide-react';

import { validationApi } from '../api/validation';
import { reportsApi } from '../api/reports';

import PageHeader from '../components/PageHeader';
import Loader from '../components/Loader';
import EmptyState from '../components/EmptyState';
import {
  ClassificationBadge,
  RiskBadge,
} from '../components/StatusBadge';

import { formatDate } from '../utils/format';


export default function ValidationHistoryPage() {

  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);

  const [selected, setSelected] = useState([]);

  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState('ALL');

  const navigate = useNavigate();


  // ============================================================
  // LOAD HISTORY
  // ============================================================

  const loadHistory = async () => {

    setLoading(true);

    try {

      const data =
        await validationApi.history();

      setHistory(
        Array.isArray(data)
          ? data
          : []
      );

    } catch (error) {

      toast.error(
        'Unable to load validation history.'
      );

    } finally {

      setLoading(false);
    }
  };


  useEffect(() => {

    loadHistory();

  }, []);


  // ============================================================
  // VERSION NUMBER
  // ============================================================
  //
  // Same startup can be validated multiple times.
  //
  // Example:
  //
  // Flower2Door — Version 3
  // Flower2Door — Version 2
  // Flower2Door — Version 1
  //

  const versionMap = useMemo(() => {

    const counters = {};

    const map = {};

    [...history]
      .sort(
        (a, b) =>
          new Date(a.createdAt) -
          new Date(b.createdAt)
      )
      .forEach((item) => {

        counters[item.startupId] =
          (counters[item.startupId] || 0) + 1;

        map[item.validationId] =
          counters[item.startupId];
      });

    return map;

  }, [history]);


  // ============================================================
  // FILTER HISTORY
  // ============================================================

  const filteredHistory = useMemo(() => {

    const term =
      search
        .trim()
        .toLowerCase();

    return history.filter((item) => {

      const matchesSearch =
        !term ||
        item.startupName
          ?.toLowerCase()
          .includes(term) ||
        item.industry
          ?.toLowerCase()
          .includes(term);

      const matchesFilter =
        filter === 'ALL' ||
        item.classification === filter;

      return (
        matchesSearch &&
        matchesFilter
      );
    });

  }, [
    history,
    search,
    filter,
  ]);


  // ============================================================
  // STATISTICS
  // ============================================================

  const statistics = useMemo(() => {

    if (!history.length) {

      return {
        total: 0,
        average: 0,
        best: 0,
      };
    }

    const scores =
      history
        .map(
          (item) =>
            Number(
              item.viabilityScore
            )
        )
        .filter(
          (score) =>
            Number.isFinite(score)
        );

    if (!scores.length) {

      return {
        total: history.length,
        average: 0,
        best: 0,
      };
    }

    const total =
      scores.reduce(
        (sum, score) =>
          sum + score,
        0
      );

    return {
      total: history.length,

      average:
        Math.round(
          total / scores.length
        ),

      best:
        Math.round(
          Math.max(...scores)
        ),
    };

  }, [history]);


  // ============================================================
  // SELECT VALIDATION
  // ============================================================

  const toggleSelect = (id) => {

    setSelected((current) => {

      if (
        current.includes(id)
      ) {

        return current.filter(
          (item) =>
            item !== id
        );
      }

      if (
        current.length >= 2
      ) {

        toast.error(
          'You can compare only two validations.'
        );

        return current;
      }

      return [
        ...current,
        id,
      ];
    });
  };


  // ============================================================
  // COMPARE
  // ============================================================

  const goCompare = () => {

    if (
      selected.length !== 2
    ) {

      toast.error(
        'Select exactly two validations to compare.'
      );

      return;
    }

    navigate(
      `/compare?a=${selected[0]}&b=${selected[1]}`
    );
  };


  // ============================================================
  // CLEAR SELECTION
  // ============================================================

  const clearSelection = () => {

    setSelected([]);
  };


  // ============================================================
  // LOADING
  // ============================================================

  if (loading) {

    return (
      <Loader
        label="Loading validation history..."
      />
    );
  }


  // ============================================================
  // PAGE
  // ============================================================

  return (

    <div>

      <PageHeader
        title="Validation History"
        subtitle="Track your completed startup validations and compare results over time."
        actions={
          <div className="flex items-center gap-2">

            {selected.length > 0 && (
              <button
                type="button"
                onClick={clearSelection}
                className="btn-outline"
              >
                <X size={16} />
                Clear
              </button>
            )}

            <button
              type="button"
              onClick={goCompare}
              disabled={
                selected.length !== 2
              }
              className="btn-outline disabled:cursor-not-allowed disabled:opacity-50"
            >
              <GitCompare size={16} />

              Compare Selected
              {' '}
              ({selected.length}/2)
            </button>

          </div>
        }
      />


      {/* ======================================================
          SUMMARY
      ====================================================== */}

      {history.length > 0 && (

        <div className="mb-6 grid grid-cols-1 gap-4 md:grid-cols-3">

          <div className="card">

            <div className="flex items-center gap-3">

              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-slate-100 text-navy-700">
                <History size={20} />
              </div>

              <div>

                <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
                  Total Validations
                </p>

                <p className="text-2xl font-extrabold text-navy-900">
                  {statistics.total}
                </p>

              </div>

            </div>

          </div>


          <div className="card">

            <div className="flex items-center gap-3">

              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-50 text-blue-600">
                <BarChart3 size={20} />
              </div>

              <div>

                <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
                  Average Score
                </p>

                <p className="text-2xl font-extrabold text-navy-900">
                  {statistics.average}
                  <span className="ml-1 text-sm font-medium text-slate-400">
                    /100
                  </span>
                </p>

              </div>

            </div>

          </div>


          <div className="card">

            <div className="flex items-center gap-3">

              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-50 text-amber-600">
                <Trophy size={20} />
              </div>

              <div>

                <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
                  Highest Score
                </p>

                <p className="text-2xl font-extrabold text-navy-900">
                  {statistics.best}
                  <span className="ml-1 text-sm font-medium text-slate-400">
                    /100
                  </span>
                </p>

              </div>

            </div>

          </div>

        </div>
      )}


      {/* ======================================================
          FILTER BAR
      ====================================================== */}

      {history.length > 0 && (

        <div className="mb-4 flex flex-col gap-3 sm:flex-row">

          <div className="relative flex-1">

            <Search
              size={17}
              className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
            />

            <input
              value={search}
              onChange={(e) =>
                setSearch(e.target.value)
              }
              placeholder="Search startup or industry..."
              className="input pl-10"
            />

          </div>


          <select
            value={filter}
            onChange={(e) =>
              setFilter(e.target.value)
            }
            className="input sm:w-56"
          >

            <option value="ALL">
              All Classifications
            </option>

            <option value="HIGH_POTENTIAL">
              High Potential
            </option>

            <option value="MODERATE_POTENTIAL">
              Moderate Potential
            </option>

            <option value="NEEDS_IMPROVEMENT">
              Needs Improvement
            </option>

            <option value="HIGH_RISK">
              High Risk
            </option>

          </select>

        </div>
      )}


      {/* ======================================================
          COMPARISON INFO
      ====================================================== */}

      {selected.length > 0 && (

        <div className="mb-4 rounded-xl border border-blue-100 bg-blue-50 px-4 py-3 text-sm text-blue-800">

          <div className="flex items-center justify-between gap-4">

            <div>

              <span className="font-semibold">
                {selected.length}/2 selected
              </span>

              <span className="ml-2 text-blue-700">
                Select two completed validations to compare their
                scores, market potential, business model,
                financial feasibility, risk and scalability.
              </span>

            </div>

            {selected.length === 2 && (

              <button
                type="button"
                onClick={goCompare}
                className="shrink-0 font-semibold hover:underline"
              >
                Compare now →
              </button>

            )}

          </div>

        </div>
      )}


      {/* ======================================================
          TABLE
      ====================================================== */}

      {filteredHistory.length === 0 ? (

        <EmptyState
          icon={History}
          title={
            history.length === 0
              ? 'No completed validations yet'
              : 'No matching validations'
          }
          description={
            history.length === 0
              ? 'Run a startup through VentureAI and its completed validation will appear here.'
              : 'Try changing your search or classification filter.'
          }
          action={
            history.length === 0 ? (
              <Link
                to="/validate/new"
                className="btn-primary mt-2"
              >
                Start New Validation
              </Link>
            ) : null
          }
        />

      ) : (

        <div className="card overflow-x-auto">

          <table className="w-full min-w-[1050px] text-left text-sm">

            <thead>

              <tr className="border-b border-slate-100 text-xs uppercase tracking-wide text-slate-400">

                <th className="pb-3 pr-4">
                  Compare
                </th>

                <th className="pb-3 pr-4">
                  Startup
                </th>

                <th className="pb-3 pr-4">
                  Version
                </th>

                <th className="pb-3 pr-4">
                  Date
                </th>

                <th className="pb-3 pr-4">
                  Score
                </th>

                <th className="pb-3 pr-4">
                  Classification
                </th>

                <th className="pb-3 pr-4">
                  Risk
                </th>

                <th className="pb-3">
                  Actions
                </th>

              </tr>

            </thead>


            <tbody>

              {filteredHistory.map(
                (item) => {

                  const score =
                    Number(
                      item.viabilityScore
                    );

                  return (

                    <tr
                      key={
                        item.validationId
                      }
                      className="border-b border-slate-50 last:border-0 hover:bg-slate-50/60"
                    >

                      {/* Compare */}
                      <td className="py-4 pr-4">

                        <input
                          type="checkbox"
                          checked={
                            selected.includes(
                              item.validationId
                            )
                          }
                          onChange={() =>
                            toggleSelect(
                              item.validationId
                            )
                          }
                          className="h-4 w-4 rounded border-slate-300"
                          aria-label={`Select ${item.startupName} for comparison`}
                        />

                      </td>


                      {/* Startup */}
                      <td className="py-4 pr-4">

                        <div>

                          <p className="font-semibold text-navy-900">
                            {item.startupName}
                          </p>

                          <p className="mt-0.5 text-xs text-slate-400">
                            {item.industry ||
                              'Industry not specified'}
                          </p>

                        </div>

                      </td>


                      {/* Version */}
                      <td className="py-4 pr-4">

                        <span className="rounded-lg bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-600">
                          Version{' '}
                          {versionMap[
                            item.validationId
                          ] || 1}
                        </span>

                      </td>


                      {/* Date */}
                      <td className="py-4 pr-4 text-slate-500">
                        {formatDate(
                          item.createdAt
                        )}
                      </td>


                      {/* Score */}
                      <td className="py-4 pr-4">

                        <span className="font-extrabold text-navy-900">
                          {Number.isFinite(score)
                            ? Math.round(score)
                            : '—'}
                        </span>

                        <span className="text-slate-400">
                          /100
                        </span>

                      </td>


                      {/* Classification */}
                      <td className="py-4 pr-4">

                        {item.classification ? (

                          <ClassificationBadge
                            classification={
                              item.classification
                            }
                          />

                        ) : (

                          <span className="text-slate-400">
                            —
                          </span>

                        )}

                      </td>


                      {/* Risk */}
                      <td className="py-4 pr-4">

                        {item.riskLevel ? (

                          <RiskBadge
                            level={
                              item.riskLevel
                            }
                          />

                        ) : (

                          <span className="text-slate-400">
                            —
                          </span>

                        )}

                      </td>


                      {/* Actions */}
                      <td className="py-4">

                        <div className="flex items-center gap-3">

                          <Link
                            to={`/validations/${item.validationId}/result`}
                            className="rounded-lg p-2 text-navy-600 transition hover:bg-slate-100 hover:text-brand-red"
                            title="View validation"
                            aria-label="View validation"
                          >
                            <Eye size={17} />
                          </Link>


                          {item.reportAvailable ? (

                            <button
                              type="button"
                              onClick={() =>
                                reportsApi.download(
                                  item.validationId
                                )
                              }
                              className="rounded-lg p-2 text-navy-600 transition hover:bg-slate-100 hover:text-brand-red"
                              title="Download validation report"
                              aria-label="Download validation report"
                            >
                              <Download
                                size={17}
                              />
                            </button>

                          ) : (

                            <span
                              className="rounded-lg p-2 text-slate-300"
                              title="Report not available"
                            >
                              <Download
                                size={17}
                              />
                            </span>

                          )}

                        </div>

                      </td>

                    </tr>

                  );

                }
              )}

            </tbody>

          </table>

        </div>

      )}

    </div>
  );
}