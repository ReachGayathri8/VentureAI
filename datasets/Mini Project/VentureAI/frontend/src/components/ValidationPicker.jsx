import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { Sparkles } from 'lucide-react';
import { validationApi } from '../api/validation';
import Loader from './Loader';
import EmptyState from './EmptyState';
import { formatDate } from '../utils/format';

/**
 * Used by sidebar-level pages (Market Intelligence, Competitors) that need a
 * specific validation to show data for, but were opened without one in the
 * URL. Lets the user pick from their validation history.
 */
export default function ValidationPicker({ toPath, title }) {
  const navigate = useNavigate();
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    validationApi.history().then(setHistory).catch(() => toast.error('Unable to load validations.')).finally(() => setLoading(false));
  }, []);

  if (loading) return <Loader label="Loading your validations..." />;

  if (history.length === 0) {
    return (
      <EmptyState
        icon={Sparkles}
        title="No validations yet"
        description="Run a startup validation first to see this page come to life."
        action={<button onClick={() => navigate('/validate/new')} className="btn-primary mt-2">Validate a Startup</button>}
      />
    );
  }

  return (
    <div>
      <p className="mb-4 text-sm text-slate-500">Choose a validation to view its {title}.</p>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {history.map((v) => (
          <button
            key={v.validationId}
            onClick={() => navigate(toPath(v.validationId))}
            className="card text-left hover:border-navy-200"
          >
            <p className="font-bold text-navy-900">{v.startupName}</p>
            <p className="text-xs text-slate-400">{v.industry} · {formatDate(v.createdAt)}</p>
            <p className="mt-2 text-sm font-semibold text-navy-700">{Math.round(v.viabilityScore)}/100</p>
          </button>
        ))}
      </div>
    </div>
  );
}
