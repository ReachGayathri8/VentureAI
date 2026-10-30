import React, { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { Download, Printer, Sparkles, Swords, Globe2, Wallet, ShieldAlert } from 'lucide-react';
import { validationApi } from '../api/validation';
import { reportsApi } from '../api/reports';
import PageHeader from '../components/PageHeader';
import Loader from '../components/Loader';
import ScoreGauge from '../components/ScoreGauge';
import ScoreBar from '../components/ScoreBar';
import { ClassificationBadge, RiskBadge } from '../components/StatusBadge';

export default function ValidationResult() {
  const { id } = useParams();
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    validationApi.get(id).then(setResult).catch(() => toast.error('Unable to load validation result.')).finally(() => setLoading(false));
  }, [id]);

  if (loading) return <Loader label="Loading validation result..." />;
  if (!result) return null;

  return (
    <div>
      <PageHeader
        title={result.startupName}
        subtitle={`Validated on ${new Date(result.createdAt).toLocaleString()}`}
        actions={
          <>
            <button onClick={() => reportsApi.download(result.id)} className="btn-outline">
              <Download size={16} /> Download PDF
            </button>
            <button onClick={() => window.print()} className="btn-outline">
              <Printer size={16} /> Print
            </button>
          </>
        }
      />

      <div className="mb-6 grid grid-cols-1 gap-6 lg:grid-cols-3">
        <div className="card flex flex-col items-center justify-center lg:col-span-1">
          <ScoreGauge score={result.viabilityScore} label={result.classification} />
          <div className="mt-4 flex flex-wrap justify-center gap-2">
            <ClassificationBadge classification={result.classification} />
            <RiskBadge level={result.riskLevel} />
          </div>
          <p className="mt-3 text-sm text-slate-500">Confidence: <span className="font-bold text-navy-900">{result.confidencePercent}%</span></p>
        </div>

        <div className="card lg:col-span-2">
          <h3 className="section-title mb-4">Score Breakdown</h3>
          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
            <ScoreBar label="Market Potential" value={result.scoreBreakdown.marketPotential} />
            <ScoreBar label="Business Model" value={result.scoreBreakdown.businessModel} />
            <ScoreBar label="Competition (advantage)" value={result.scoreBreakdown.competition} />
            <ScoreBar label="Financial Feasibility" value={result.scoreBreakdown.financialFeasibility} />
            <ScoreBar label="Scalability" value={result.scoreBreakdown.scalability} />
          </div>
          <div className="mt-6 grid grid-cols-3 gap-3 text-center">
            <MiniStat label="Market Potential" value={result.marketPotentialLabel} />
            <MiniStat label="Financial Feasibility" value={result.financialFeasibilityLabel} />
            <MiniStat label="Scalability" value={result.scalabilityLabel} />
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
        <NavCard to={`/validations/${result.id}/swot`} icon={Sparkles} title="SWOT Analysis" desc="Strengths, weaknesses, opportunities & threats" />
        <NavCard to={`/validations/${result.id}/competitors`} icon={Swords} title="Competitor Intelligence" desc="How you stack up against the competition" />
        <NavCard to={`/validations/${result.id}/market-intelligence`} icon={Globe2} title="Market Intelligence" desc="Demand, growth and opportunity areas" />
        <NavCard to={`/financials/${result.startupId}`} icon={Wallet} title="Financial Analysis" desc="Revenue, P&L, cash flow & break-even" />
        <NavCard to={`/validations/${result.id}/recommendations`} icon={Sparkles} title="AI Recommendations" desc="Personalized next steps from VentureAI" />
        <NavCard to={`/validations/${result.id}/risk`} icon={ShieldAlert} title="Risk Analysis" desc="Category-by-category risk assessment" />
      </div>
    </div>
  );
}

function MiniStat({ label, value }) {
  return (
    <div className="rounded-xl bg-slate-50 p-3">
      <p className="text-xs text-slate-400">{label}</p>
      <p className="font-bold text-navy-900">{value}</p>
    </div>
  );
}

function NavCard({ to, icon: Icon, title, desc }) {
  return (
    <Link to={to} className="card flex flex-col gap-3 hover:border-navy-200">
      <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-navy-50 text-navy-700">
        <Icon size={20} />
      </div>
      <div>
        <h4 className="font-bold text-navy-900">{title}</h4>
        <p className="mt-1 text-xs text-slate-500">{desc}</p>
      </div>
    </Link>
  );
}
