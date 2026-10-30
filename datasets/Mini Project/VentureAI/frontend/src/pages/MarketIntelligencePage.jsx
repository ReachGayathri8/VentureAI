import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid } from 'recharts';
import { validationApi } from '../api/validation';
import PageHeader from '../components/PageHeader';
import Loader from '../components/Loader';
import ValidationPicker from '../components/ValidationPicker';
import ChartCard from '../charts/ChartCard';

export default function MarketIntelligencePage() {
  const { id } = useParams();

  if (!id) {
    return (
      <div>
        <PageHeader title="Market Intelligence" subtitle="Pick a validation to see its market opportunity analysis." />
        <ValidationPicker toPath={(vid) => `/validations/${vid}/market-intelligence`} title="market intelligence" />
      </div>
    );
  }
  return <MarketIntelligenceContent id={id} />;
}

function MarketIntelligenceContent({ id }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    validationApi.marketOpportunity(id).then(setData).catch(() => toast.error('Unable to load market intelligence.')).finally(() => setLoading(false));
  }, [id]);

  if (loading) return <Loader label="Loading market intelligence..." />;
  if (!data) return null;

  const scoreData = [
    { name: 'Market Growth', value: Math.round(data.marketGrowthScore) },
    { name: 'Demand', value: Math.round(data.demandScore) },
    { name: 'Competition', value: Math.round(data.competitionScore) },
  ];

  return (
    <div>
      <PageHeader title="Market Intelligence" subtitle={`Target Market: ${data.targetMarket}`} />

      {data.aiEstimated && (
        <div className="mb-6 rounded-xl border border-amber-200 bg-amber-50 p-3 text-xs font-medium text-amber-800">
          Figures below are AI-generated estimates — no external market-data feed is connected in this build.
        </div>
      )}

      <div className="mb-6 grid grid-cols-1 gap-5 sm:grid-cols-2">
        <div className="card">
          <h4 className="mb-1 text-sm font-bold text-navy-900">Market Demand</h4>
          <p className="text-2xl font-extrabold text-navy-900">{data.marketDemand}</p>
        </div>
        <div className="card">
          <h4 className="mb-1 text-sm font-bold text-navy-900">Growth Potential</h4>
          <p className="text-2xl font-extrabold text-navy-900">{data.growthPotential}</p>
        </div>
      </div>

      <ChartCard title="Market Growth, Demand & Competition" subtitle="0-100 scale" >
        <BarChart data={scoreData}>
          <CartesianGrid strokeDasharray="3 3" stroke="#eef1f8" />
          <XAxis dataKey="name" tick={{ fontSize: 12 }} />
          <YAxis domain={[0, 100]} tick={{ fontSize: 12 }} />
          <Tooltip />
          <Bar dataKey="value" fill="#3b5299" radius={[6, 6, 0, 0]} />
        </BarChart>
      </ChartCard>

      <div className="mt-6 grid grid-cols-1 gap-5 sm:grid-cols-2">
        <ListCard title="Customer Segments" items={data.customerSegments} />
        <ListCard title="Market Trends" items={data.marketTrends} />
        <ListCard title="Opportunity Areas" items={data.opportunityAreas} />
        <ListCard title="Major Risks" items={data.majorRisks} />
      </div>
    </div>
  );
}

function ListCard({ title, items = [] }) {
  return (
    <div className="card">
      <h4 className="mb-3 text-sm font-bold text-navy-900">{title}</h4>
      <ul className="space-y-2">
        {items.map((item, idx) => (
          <li key={idx} className="flex gap-2 text-sm text-slate-600">
            <span className="mt-1.5 h-1.5 w-1.5 flex-shrink-0 rounded-full bg-navy-400" />
            {item}
          </li>
        ))}
      </ul>
    </div>
  );
}
