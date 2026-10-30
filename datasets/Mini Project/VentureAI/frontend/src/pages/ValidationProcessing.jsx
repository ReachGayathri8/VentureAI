import React, { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { CheckCircle2, Circle, Loader2, AlertTriangle } from 'lucide-react';
import { validationApi } from '../api/validation';
import { extractErrorMessage } from '../api/client';

const STAGES = [
  'Input validation',
  'NLP preprocessing',
  'Keyword extraction',
  'Feature generation',
  'BART prediction',
  'Market analysis',
  'Competitor analysis',
  'SWOT generation',
  'Financial analysis',
  'AI recommendations',
  'Report generation',
];

export default function ValidationProcessing() {
  const { startupId } = useParams();
  const navigate = useNavigate();
  const [activeStage, setActiveStage] = useState(0);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);
  const requestFired = useRef(false);

  useEffect(() => {
    const interval = setInterval(() => {
      setActiveStage((s) => Math.min(s + 1, STAGES.length - 1));
    }, 550);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    if (requestFired.current) return;
    requestFired.current = true;

    validationApi
      .run(startupId)
      .then((data) => setResult(data))
      .catch((err) => {
        setError(extractErrorMessage(err, 'Unable to analyze startup. Please try again.'));
      });
  }, [startupId]);

  // Navigate once BOTH the checklist animation has finished AND the real
  // backend result has arrived — whichever finishes last. This guarantees
  // we never "fake" a result: if the AI/ML calls take longer than the
  // animation, we simply keep waiting at the final stage until they return.
  useEffect(() => {
    if (activeStage === STAGES.length - 1 && result) {
      const timeout = setTimeout(() => navigate(`/validations/${result.id}/result`), 500);
      return () => clearTimeout(timeout);
    }
  }, [activeStage, result, navigate]);

  if (error) {
    return (
      <div className="mx-auto flex max-w-md flex-col items-center justify-center py-24 text-center">
        <AlertTriangle className="mb-4 text-brand-red" size={40} />
        <h2 className="text-xl font-bold text-navy-900">Validation failed</h2>
        <p className="mt-2 text-sm text-slate-500">{error}</p>
        <button onClick={() => navigate(-1)} className="btn-primary mt-6">Go Back</button>
      </div>
    );
  }

  return (
    <div className="mx-auto flex max-w-lg flex-col items-center justify-center py-16 text-center">
      <Loader2 className="mb-6 animate-spin text-brand-red" size={48} />
      <h2 className="text-2xl font-extrabold text-navy-900">Analyzing Startup Idea...</h2>
      <p className="mb-8 mt-2 text-sm text-slate-500">
        Running your idea through the full VentureAI pipeline — this usually takes a few seconds.
      </p>
      <div className="w-full space-y-3 text-left">
        {STAGES.map((stage, idx) => (
          <div key={stage} className="flex items-center gap-3">
            {idx < activeStage ? (
              <CheckCircle2 className="flex-shrink-0 text-emerald-500" size={20} />
            ) : idx === activeStage ? (
              <Loader2 className="flex-shrink-0 animate-spin text-navy-600" size={20} />
            ) : (
              <Circle className="flex-shrink-0 text-slate-300" size={20} />
            )}
            <span className={`text-sm ${idx <= activeStage ? 'font-medium text-navy-900' : 'text-slate-400'}`}>{stage}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
