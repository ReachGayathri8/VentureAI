import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { PieChart, Pie, Cell, Tooltip, Legend } from 'recharts';
import { validationApi } from '../api/validation';
import PageHeader from '../components/PageHeader';
import Loader from '../components/Loader';
import ValidationPicker from '../components/ValidationPicker';
import { RiskBadge } from '../components/StatusBadge';
import ChartCard from '../charts/ChartCard';

const RISK_COLORS = { LOW: '#059669', MEDIUM: '#d97706', HIGH: '#dc2626' };

export default function RiskAnalysisPage() {
  const { id } = useParams();
  if (!id) {
    return (
      <div>
        <PageHeader title="Risk Analysis" subtitle="Pick a validation to see its risk breakdown." />
        <ValidationPicker toPath={(vid) => `/validations/${vid}/risk`} title="risk analysis" />
      </div>
    );
  }
  return <RiskAnalysisContent id={id} />;
}

function RiskAnalysisContent({ id }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    validationApi.risks(id).then(setData).catch(() => toast.error('Unable to load risk analysis.')).finally(() => setLoading(false));
  }, [id]);

  if (loading) return <Loader label="Loading risk analysis..." />;
  if (!data) return null;

  const distribution = ['LOW', 'MEDIUM', 'HIGH'].map((level) => ({
    name: level,
    value: data.categories.filter((c) => c.riskLevel === level).length,
  })).filter((d) => d.value > 0);

  return (
    <div>
      <PageHeader
        title="Risk Analysis"
        subtitle="Category-by-category risk assessment with mitigation strategies."
        actions={<RiskBadge level={data.overallRiskLevel} />}
      />

      <div className="mb-6 grid grid-cols-1 gap-6 lg:grid-cols-3">
        <div className="card flex flex-col items-center justify-center lg:col-span-1">
          <p className="text-sm text-slate-500">Overall Risk Score</p>
          <p className="mt-2 text-4xl font-extrabold text-navy-900">{data.overallRiskScore}</p>
          <p className="text-xs text-slate-400">out of 100</p>
        </div>
        <div className="card lg:col-span-2">
          <ChartCard title="Risk Distribution" height={220}>
            <PieChart>
              <Pie data={distribution} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={80} label>
                {distribution.map((d) => (
                  <Cell key={d.name} fill={RISK_COLORS[d.name]} />
                ))}
              </Pie>
              <Tooltip />
              <Legend />
            </PieChart>
          </ChartCard>
        </div>
      </div>

      <div className="card overflow-x-auto">
        <table className="w-full min-w-[720px] text-left text-sm">
          <thead>
            <tr className="border-b border-slate-100 text-xs uppercase tracking-wide text-slate-400">
              <th className="pb-3 pr-4">Category</th>
              <th className="pb-3 pr-4">Risk Level</th>
              <th className="pb-3 pr-4">Impact</th>
              <th className="pb-3">Mitigation Strategy</th>
            </tr>
          </thead>
          <tbody>
            {data.categories.map((c) => (
              <tr key={c.category} className="border-b border-slate-50 last:border-0 align-top">
                <td className="py-3 pr-4 font-semibold text-navy-900">{c.category}</td>
                <td className="py-3 pr-4"><RiskBadge level={c.riskLevel} /></td>
                <td className="py-3 pr-4"><RiskBadge level={c.impact} /></td>
                <td className="py-3 text-slate-600">{c.mitigationStrategy}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
