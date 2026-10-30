import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { Sparkles, MapPin, Building2 } from 'lucide-react';
import { startupsApi } from '../api/startups';
import { validationApi } from '../api/validation';
import PageHeader from '../components/PageHeader';
import Loader from '../components/Loader';
import { ClassificationBadge } from '../components/StatusBadge';

export default function StartupDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [startup, setStartup] = useState(null);
  const [running, setRunning] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    startupsApi.get(id).then(setStartup).catch(() => toast.error('Unable to load startup.')).finally(() => setLoading(false));
  }, [id]);

  const runValidation = async () => {
    setRunning(true);
    navigate(`/validate/${id}/processing`);
  };

  if (loading) return <Loader label="Loading startup..." />;
  if (!startup) return null;

  return (
    <div>
      <PageHeader
        title={startup.name}
        subtitle={startup.startupIdea}
        actions={
          <button onClick={runValidation} disabled={running} className="btn-accent">
            <Sparkles size={16} /> {startup.latestViabilityScore != null ? 'Re-run Validation' : 'Start AI Validation'}
          </button>
        }
      />

      <div className="mb-6 flex flex-wrap gap-3 text-sm text-slate-500">
        <span className="flex items-center gap-1.5"><Building2 size={16} /> {startup.industry} · {startup.businessCategory}</span>
        <span className="flex items-center gap-1.5"><MapPin size={16} /> {startup.location || 'Location not set'}</span>
        {startup.latestClassification && <ClassificationBadge classification={startup.latestClassification} />}
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div className="card">
          <h3 className="section-title mb-3">Business Overview</h3>
          <Field label="Problem" value={startup.problem} />
          <Field label="Solution" value={startup.solution} />
          <Field label="Target Market" value={startup.targetMarket} />
          <Field label="Business Model" value={startup.businessModel} />
          <Field label="Revenue Model" value={startup.revenueModel} />
          <Field label="Unique Value Proposition" value={startup.uniqueValueProposition} />
        </div>

        <div className="card">
          <h3 className="section-title mb-3">Competitors</h3>
          {startup.competitors?.length ? (
            <ul className="space-y-3">
              {startup.competitors.map((c) => (
                <li key={c.id} className="rounded-xl border border-slate-100 p-3">
                  <p className="font-semibold text-navy-900">{c.name}</p>
                  <p className="text-sm text-slate-500">{c.description}</p>
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-sm text-slate-400">No competitors added yet.</p>
          )}
        </div>
      </div>
    </div>
  );
}

function Field({ label, value }) {
  if (!value) return null;
  return (
    <div className="mb-3">
      <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">{label}</p>
      <p className="text-sm text-slate-700">{value}</p>
    </div>
  );
}
