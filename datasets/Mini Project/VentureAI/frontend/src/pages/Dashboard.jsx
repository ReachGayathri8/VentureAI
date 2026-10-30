import React, { useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  LineChart,
  Line,
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  Legend,
} from 'recharts';
import {
  Target,
  Gauge,
  Trophy,
  FileText,
  Sparkles,
  Eye,
  Download,
} from 'lucide-react';
import toast from 'react-hot-toast';

import { dashboardApi } from '../api/dashboard';
import { reportsApi } from '../api/reports';

import KpiCard from '../components/KpiCard';
import ChartCard from '../charts/ChartCard';
import PageHeader from '../components/PageHeader';
import Loader from '../components/Loader';
import EmptyState from '../components/EmptyState';

import { ClassificationBadge, RiskBadge } from '../components/StatusBadge';
import { formatDate } from '../utils/format';
import { useAuth } from '../auth/AuthContext';

const COLORS = [
  '#1f2c58',
  '#c81e2d',
  '#3b5299',
  '#d97706',
  '#059669',
  '#8697cb',
];

export default function Dashboard() {
  const { user } = useAuth();

  const [data, setData] = React.useState(null);
  const [loading, setLoading] = React.useState(true);

  useEffect(() => {
    dashboardApi
      .get()
      .then(setData)
      .catch(() => {
        toast.error('Unable to load dashboard.');
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  if (loading) {
    return <Loader label="Loading your dashboard..." />;
  }

  const industryData = Object.entries(
    data?.industryDistribution || {}
  ).map(([name, value]) => ({
    name,
    value,
  }));

  return (
    <div>
      {/* PAGE HEADER */}
      <PageHeader
        title={`Welcome back, ${
          user?.fullName?.split(' ')[0] || 'there'
        }`}
        subtitle="Here's how your startup validations are performing."
        actions={
          <Link to="/validate/new" className="btn-accent">
            <Sparkles size={16} />
            Validate New Startup
          </Link>
        }
      />

      {/* KPI CARDS */}
      <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <KpiCard
          label="Total Validations"
          value={data?.totalValidations ?? 0}
          icon={Target}
          accent="navy"
        />

        <KpiCard
          label="Average Viability Score"
          value={`${data?.averageViabilityScore ?? 0}`}
          sublabel="out of 100"
          icon={Gauge}
          accent="red"
        />

        <KpiCard
          label="High Potential Ideas"
          value={data?.highPotentialCount ?? 0}
          icon={Trophy}
          accent="emerald"
        />

        <KpiCard
          label="Reports Generated"
          value={data?.reportsGenerated ?? 0}
          icon={FileText}
          accent="amber"
        />
      </div>

      {/* EMPTY STATE */}
      {!data || data.totalValidations === 0 ? (
        <EmptyState
          icon={Sparkles}
          title="No validations yet"
          description="Run your first AI startup validation to see your dashboard come to life."
          action={
            <Link
              to="/validate/new"
              className="btn-primary mt-2"
            >
              Validate My First Idea
            </Link>
          }
        />
      ) : (
        <>
          {/* ANALYTICS */}
          <div className="mb-6 grid grid-cols-1 gap-6 lg:grid-cols-2">

            {/* VIABILITY HISTORY */}
            <ChartCard
              title="Viability Score History"
              subtitle="Score trend across your validations"
            >
              <LineChart data={data.viabilityScoreHistory}>
                <CartesianGrid
                  strokeDasharray="3 3"
                  stroke="#eef1f8"
                />

                <XAxis
                  dataKey="label"
                  tick={{ fontSize: 12 }}
                />

                <YAxis
                  domain={[0, 100]}
                  tick={{ fontSize: 12 }}
                />

                <Tooltip />

                <Line
                  type="monotone"
                  dataKey="value"
                  stroke="#c81e2d"
                  strokeWidth={2.5}
                  dot={{ r: 3 }}
                  name="Viability Score"
                />
              </LineChart>
            </ChartCard>

            {/* INDUSTRY DISTRIBUTION */}
            <ChartCard
              title="Industry Distribution"
              subtitle="Your validations by industry"
            >
              {industryData.length === 1 ? (
                <div className="flex h-full min-h-[260px] flex-col items-center justify-center">
                  <div className="text-4xl font-bold text-navy-900">
                    {industryData[0].value}
                  </div>

                  <p className="mt-2 text-sm text-slate-500">
                    validation
                    {industryData[0].value !== 1 ? 's' : ''} in
                  </p>

                  <p className="mt-1 text-lg font-semibold text-navy-900">
                    {industryData[0].name}
                  </p>
                </div>
              ) : (
                <PieChart>
                  <Pie
                    data={industryData}
                    dataKey="value"
                    nameKey="name"
                    cx="50%"
                    cy="50%"
                    outerRadius={90}
                    label
                  >
                    {industryData.map((entry, index) => (
                      <Cell
                        key={entry.name}
                        fill={COLORS[index % COLORS.length]}
                      />
                    ))}
                  </Pie>

                  <Tooltip />
                  <Legend />
                </PieChart>
              )}
            </ChartCard>

            {/* MARKET OPPORTUNITY */}
            <ChartCard
              title="Market Opportunity Across Validations"
              subtitle="Market opportunity score for each validation"
            >
              <BarChart data={data.marketOpportunityTrend}>
                <CartesianGrid
                  strokeDasharray="3 3"
                  stroke="#eef1f8"
                />

                <XAxis
                  dataKey="label"
                  tick={{ fontSize: 12 }}
                />

                <YAxis
                  domain={[0, 100]}
                  tick={{ fontSize: 12 }}
                />

                <Tooltip />

                <Bar
                  dataKey="value"
                  fill="#3b5299"
                  radius={[6, 6, 0, 0]}
                  name="Market Score"
                />
              </BarChart>
            </ChartCard>

            {/* FINANCIAL HEALTH */}
            <ChartCard
              title="Financial Health Across Validations"
              subtitle="Financial health score for each validation"
            >
              <LineChart data={data.financialProjectionTrend}>
                <CartesianGrid
                  strokeDasharray="3 3"
                  stroke="#eef1f8"
                />

                <XAxis
                  dataKey="label"
                  tick={{ fontSize: 12 }}
                />

                <YAxis
                  domain={[0, 100]}
                  tick={{ fontSize: 12 }}
                />

                <Tooltip />

                <Line
                  type="monotone"
                  dataKey="value"
                  stroke="#059669"
                  strokeWidth={2.5}
                  dot={{ r: 3 }}
                  name="Financial Score"
                />
              </LineChart>
            </ChartCard>
          </div>

          {/* RECENT VALIDATIONS */}
          <div className="card overflow-x-auto">
            <h3 className="mb-4 text-base font-bold text-navy-900">
              Recent Startup Validations
            </h3>

            <table className="w-full min-w-[720px] text-left text-sm">
              <thead>
                <tr className="border-b border-slate-100 text-xs uppercase tracking-wide text-slate-400">
                  <th className="pb-3 pr-4">Startup</th>
                  <th className="pb-3 pr-4">Industry</th>
                  <th className="pb-3 pr-4">Viability Score</th>
                  <th className="pb-3 pr-4">Risk Level</th>
                  <th className="pb-3 pr-4">Date</th>
                  <th className="pb-3 pr-4">Status</th>
                  <th className="pb-3">Actions</th>
                </tr>
              </thead>

              <tbody>
                {data.recentValidations.map((v) => (
                  <tr
                    key={v.validationId}
                    className="border-b border-slate-50 last:border-0"
                  >
                    {/* STARTUP */}
                    <td className="py-3 pr-4 font-semibold text-navy-900">
                      {v.startupName}
                    </td>

                    {/* INDUSTRY */}
                    <td className="py-3 pr-4 text-slate-500">
                      {v.industry || 'Other'}
                    </td>

                    {/* SCORE */}
                    <td className="py-3 pr-4">
                      <span className="font-bold text-navy-900">
                        {Math.round(v.viabilityScore)}
                      </span>

                      <span className="text-slate-400">
                        /100
                      </span>
                    </td>

                    {/* RISK */}
                    <td className="py-3 pr-4">
                      <RiskBadge level={v.riskLevel} />
                    </td>

                    {/* DATE */}
                    <td className="py-3 pr-4 text-slate-500">
                      {formatDate(v.createdAt)}
                    </td>

                    {/* STATUS */}
                    <td className="py-3 pr-4">
                      <ClassificationBadge
                        classification={v.classification}
                      />
                    </td>

                    {/* ACTIONS */}
                    <td className="flex items-center gap-3 py-3">
                      <Link
                        to={`/validations/${v.validationId}/result`}
                        className="text-navy-600 hover:text-brand-red"
                        title="View Validation"
                      >
                        <Eye size={16} />
                      </Link>

                      {v.reportGenerated && (
                        <button
                          onClick={() =>
                            reportsApi.download(v.validationId)
                          }
                          className="text-navy-600 hover:text-brand-red"
                          title="Download PDF Report"
                        >
                          <Download size={16} />
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}
