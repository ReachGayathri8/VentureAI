import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { Rocket } from 'lucide-react';
import { startupsApi } from '../api/startups';
import Loader from './Loader';
import EmptyState from './EmptyState';

export default function StartupPicker({ toPath, title }) {
  const navigate = useNavigate();
  const [startups, setStartups] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    startupsApi.list().then(setStartups).catch(() => toast.error('Unable to load startups.')).finally(() => setLoading(false));
  }, []);

  if (loading) return <Loader label="Loading your startups..." />;

  if (startups.length === 0) {
    return (
      <EmptyState
        icon={Rocket}
        title="No startups yet"
        description="Create a startup and run a validation first to unlock this page."
        action={<button onClick={() => navigate('/validate/new')} className="btn-primary mt-2">Create Startup</button>}
      />
    );
  }

  return (
    <div>
      <p className="mb-4 text-sm text-slate-500">Choose a startup to view its {title}.</p>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {startups.map((s) => (
          <button key={s.id} onClick={() => navigate(toPath(s.id))} className="card text-left hover:border-navy-200">
            <p className="font-bold text-navy-900">{s.name}</p>
            <p className="text-xs text-slate-400">{s.industry}</p>
          </button>
        ))}
      </div>
    </div>
  );
}
