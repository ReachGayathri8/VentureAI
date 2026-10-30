import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { FileText, Download, Eye } from 'lucide-react';
import { validationApi } from '../api/validation';
import { reportsApi } from '../api/reports';
import PageHeader from '../components/PageHeader';
import Loader from '../components/Loader';
import EmptyState from '../components/EmptyState';
import { ClassificationBadge } from '../components/StatusBadge';
import { formatDate } from '../utils/format';

export default function ReportsPage() {
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    validationApi.history().then(setHistory).catch(() => toast.error('Unable to load reports.')).finally(() => setLoading(false));
  }, []);

  if (loading) return <Loader label="Loading reports..." />;

  return (
    <div>
      <PageHeader title="Reports" subtitle="Every AI Startup Validation Report you've generated." />
      {history.length === 0 ? (
        <EmptyState icon={FileText} title="No reports yet" description="Run a validation to generate your first downloadable PDF report." />
      ) : (
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {history.map((v) => (
            <div key={v.validationId} className="card flex flex-col">
              <div className="mb-3 flex items-start justify-between">
                <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-navy-50 text-navy-700">
                  <FileText size={20} />
                </div>
                <ClassificationBadge classification={v.classification} />
              </div>
              <h3 className="font-bold text-navy-900">{v.startupName}</h3>
              <p className="mb-4 text-xs text-slate-400">{v.industry} · {formatDate(v.createdAt)}</p>
              <div className="mt-auto flex gap-3 border-t border-slate-100 pt-3">
                <Link to={`/validations/${v.validationId}/result`} className="btn-outline flex-1 text-xs">
                  <Eye size={14} /> View
                </Link>
                <button onClick={() => reportsApi.download(v.validationId)} className="btn-primary flex-1 text-xs">
                  <Download size={14} /> Download
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
