import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import {
  Rocket,
  Sparkles,
  Trash2,
  Eye,
  Pencil,
  CheckCircle2,
  Clock3,
} from 'lucide-react';

import { startupsApi } from '../api/startups';
import PageHeader from '../components/PageHeader';
import Loader from '../components/Loader';
import EmptyState from '../components/EmptyState';
import { ClassificationBadge } from '../components/StatusBadge';
import { formatDate } from '../utils/format';

export default function MyStartups() {
  const [startups, setStartups] = useState([]);
  const [loading, setLoading] = useState(true);
  const [deletingId, setDeletingId] = useState(null);

  const load = async () => {
    setLoading(true);

    try {
      const data = await startupsApi.list();
      setStartups(Array.isArray(data) ? data : []);
    } catch (error) {
      toast.error('Unable to load startups.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const handleDelete = async (startup) => {
    const confirmed = window.confirm(
      `Delete "${startup.name}"?\n\n` +
      'This will permanently delete the startup and its validation, ' +
      'competitors, financial projections and generated report records.'
    );

    if (!confirmed) return;

    setDeletingId(startup.id);

    try {
      await startupsApi.remove(startup.id);

      setStartups((current) =>
        current.filter((item) => item.id !== startup.id)
      );

      toast.success('Startup deleted successfully.');
    } catch (error) {
      toast.error(
        error?.response?.data?.message ||
          'Unable to delete startup. Please try again.'
      );
    } finally {
      setDeletingId(null);
    }
  };

  if (loading) {
    return <Loader label="Loading your startups..." />;
  }

  return (
    <div>
      <PageHeader
        title="My Startups"
        subtitle="Manage your startup ideas and their validation results."
        actions={
          <Link
            to="/validate/new"
            className="btn-accent"
          >
            <Sparkles size={16} />
            New Startup
          </Link>
        }
      />

      {startups.length === 0 ? (
        <EmptyState
          icon={Rocket}
          title="No startups yet"
          description="Create your first startup idea and run it through VentureAI."
          action={
            <Link
              to="/validate/new"
              className="btn-primary mt-2"
            >
              Create Startup
            </Link>
          }
        />
      ) : (
        <>
          {/* Summary */}
          <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="card">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600">
                  <CheckCircle2 size={20} />
                </div>

                <div>
                  <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
                    Validated
                  </p>

                  <p className="text-xl font-bold text-navy-900">
                    {
                      startups.filter(
                        (s) => s.latestViabilityScore != null
                      ).length
                    }
                  </p>
                </div>
              </div>
            </div>

            <div className="card">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-slate-100 text-slate-500">
                  <Clock3 size={20} />
                </div>

                <div>
                  <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
                    Awaiting Validation
                  </p>

                  <p className="text-xl font-bold text-navy-900">
                    {
                      startups.filter(
                        (s) => s.latestViabilityScore == null
                      ).length
                    }
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* Startup cards */}
          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-3">
            {startups.map((startup) => {
              const validated =
                startup.latestViabilityScore != null;

              const score =
                validated
                  ? Math.round(
                      Number(
                        startup.latestViabilityScore
                      )
                    )
                  : null;

              return (
                <div
                  key={startup.id}
                  className="card flex flex-col transition-shadow hover:shadow-cardHover"
                >
                  {/* Header */}
                  <div className="mb-4 flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <h3 className="truncate font-bold text-navy-900">
                        {startup.name}
                      </h3>

                      <p className="mt-1 text-xs text-slate-400">
                        {startup.industry || 'Industry not specified'}
                      </p>
                    </div>

                    {validated ? (
                      startup.latestClassification ? (
                        <ClassificationBadge
                          classification={
                            startup.latestClassification
                          }
                        />
                      ) : (
                        <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-700">
                          <CheckCircle2 size={13} />
                          VALIDATED
                        </span>
                      )
                    ) : (
                      <span className="inline-flex shrink-0 items-center gap-1 rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-500">
                        <Clock3 size={13} />
                        NOT VALIDATED
                      </span>
                    )}
                  </div>

                  {/* Idea */}
                  <div className="mb-5 flex-1">
                    <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-slate-400">
                      Startup Idea
                    </p>

                    <p className="line-clamp-3 text-sm leading-6 text-slate-600">
                      {startup.startupIdea ||
                        'No startup idea provided.'}
                    </p>
                  </div>

                  {/* Score */}
                  {validated ? (
                    <div className="mb-5 rounded-xl bg-slate-50 p-4">
                      <div className="flex items-end justify-between">
                        <div>
                          <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
                            Viability Score
                          </p>

                          <p className="mt-1 text-2xl font-extrabold text-navy-900">
                            {score}
                            <span className="ml-1 text-sm font-medium text-slate-400">
                              /100
                            </span>
                          </p>
                        </div>

                        <div className="flex items-center gap-3">
                          <Link
                            to={`/validate/new?edit=${startup.id}`}
                            className="text-xs font-semibold text-navy-600 hover:text-brand-red"
                          >
                            Edit
                          </Link>

                          <Link
                            to={`/startups/${startup.id}`}
                            className="text-xs font-semibold text-navy-600 hover:text-brand-red"
                          >
                            View details
                          </Link>
                        </div>
                      </div>
                    </div>
                  ) : (
                    <div className="mb-5 rounded-xl border border-dashed border-slate-200 bg-slate-50 p-4">
                      <p className="text-sm font-semibold text-navy-900">
                        Validation not available
                      </p>

                      <p className="mt-1 text-xs leading-5 text-slate-500">
                        Complete the AI validation before a
                        viability score can be displayed.
                      </p>
                    </div>
                  )}

                  {/* Footer */}
                  <div className="flex items-center justify-between border-t border-slate-100 pt-3">
                    <span className="text-xs text-slate-400">
                      Created {formatDate(startup.createdAt)}
                    </span>

                    <div className="flex items-center gap-3">
                      <Link
                        to={`/validate/new?edit=${startup.id}`}
                        className="rounded-lg p-2 text-slate-400 transition hover:bg-slate-50 hover:text-navy-700"
                        title="Edit startup"
                        aria-label={`Edit ${startup.name}`}
                      >
                        <Pencil size={17} />
                      </Link>

                      <Link
                        to={`/startups/${startup.id}`}
                        className="rounded-lg p-2 text-slate-400 transition hover:bg-slate-50 hover:text-navy-700"
                        title="View startup details"
                        aria-label="View startup details"
                      >
                        <Eye size={17} />
                      </Link>

                      <button
                        type="button"
                        onClick={() =>
                          handleDelete(startup)
                        }
                        disabled={
                          deletingId === startup.id
                        }
                        className="rounded-lg p-2 text-slate-400 transition hover:bg-red-50 hover:text-brand-red disabled:cursor-not-allowed disabled:opacity-50"
                        title="Delete startup"
                        aria-label="Delete startup"
                      >
                        {deletingId === startup.id ? (
                          <span className="block h-[17px] w-[17px] animate-spin rounded-full border-2 border-slate-300 border-t-brand-red" />
                        ) : (
                          <Trash2 size={17} />
                        )}
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </>
      )}
    </div>
  );
}
