import React, { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { GitCompare } from 'lucide-react';
import { validationApi } from '../api/validation';
import PageHeader from '../components/PageHeader';
import Loader from '../components/Loader';
import EmptyState from '../components/EmptyState';
import { ClassificationBadge } from '../components/StatusBadge';

export default function CompareStartupsPage() {
  const [params] = useSearchParams();
  const a = params.get('a');
  const b = params.get('b');
  const [resultA, setResultA] = useState(null);
  const [resultB, setResultB] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!a || !b) {
      setLoading(false);
      return;
    }
    Promise.all([validationApi.get(a), validationApi.get(b)])
      .then(([ra, rb]) => { setResultA(ra); setResultB(rb); })
      .catch(() => toast.error('Unable to load validations for comparison.'))
      .finally(() => setLoading(false));
  }, [a, b]);

  if (loading) return <Loader label="Loading comparison..." />;

  if (!a || !b || !resultA || !resultB) {
    return (
      <div>
        <PageHeader title="Compare Validations" subtitle="Select two validations from your history to compare them here." />
        <EmptyState icon={GitCompare} title="Nothing to compare yet" description="Go to Validation History, select two entries, then click Compare Selected." />
      </div>
    );
  }

  const metrics = [
    ['Viability Score', (r) => `${Math.round(r.viabilityScore)}/100`],
    ['Classification', (r) => r.classification],
    ['Confidence', (r) => `${r.confidencePercent}%`],
    ['Risk Level', (r) => r.riskLevel],
    ['Market Potential', (r) => `${Math.round(r.scoreBreakdown.marketPotential)}/100`],
    ['Business Model', (r) => `${Math.round(r.scoreBreakdown.businessModel)}/100`],
    ['Competition (advantage)', (r) => `${Math.round(r.scoreBreakdown.competition)}/100`],
    ['Financial Feasibility', (r) => `${Math.round(r.scoreBreakdown.financialFeasibility)}/100`],
    ['Scalability', (r) => `${Math.round(r.scoreBreakdown.scalability)}/100`],
  ];

  return (
    <div>
      <PageHeader title="Compare Validations" subtitle={`${resultA.startupName} (Version 1) vs ${resultB.startupName} (Version 2)`} />
      <div className="card overflow-x-auto">
        <table className="w-full min-w-[600px] text-left text-sm">
          <thead>
            <tr className="border-b border-slate-100 text-xs uppercase tracking-wide text-slate-400">
              <th className="pb-3 pr-4">Metric</th>
              <th className="pb-3 pr-4">{resultA.startupName}</th>
              <th className="pb-3">{resultB.startupName}</th>
            </tr>
          </thead>
          <tbody>
            {metrics.map(([label, fn]) => (
              <tr key={label} className="border-b border-slate-50 last:border-0">
                <td className="py-3 pr-4 font-medium text-slate-600">{label}</td>
                <td className="py-3 pr-4 font-semibold text-navy-900">
                  {label === 'Classification' ? <ClassificationBadge classification={fn(resultA)} /> : fn(resultA)}
                </td>
                <td className="py-3 font-semibold text-navy-900">
                  {label === 'Classification' ? <ClassificationBadge classification={fn(resultB)} /> : fn(resultB)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
