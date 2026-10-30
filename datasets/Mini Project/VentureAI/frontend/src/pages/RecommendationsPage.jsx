import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import {
  Briefcase, Megaphone, Tag, DollarSign, Package, TrendingUp, ShieldCheck, Landmark,
} from 'lucide-react';
import { validationApi } from '../api/validation';
import PageHeader from '../components/PageHeader';
import Loader from '../components/Loader';
import ValidationPicker from '../components/ValidationPicker';

const SECTIONS = [
  { key: 'business', title: 'Business Recommendations', icon: Briefcase },
  { key: 'marketing', title: 'Marketing Recommendations', icon: Megaphone },
  { key: 'pricing', title: 'Pricing Recommendations', icon: Tag },
  { key: 'revenueModel', title: 'Revenue Model Recommendations', icon: DollarSign },
  { key: 'product', title: 'Product Recommendations', icon: Package },
  { key: 'growthStrategy', title: 'Growth Strategy', icon: TrendingUp },
  { key: 'riskMitigation', title: 'Risk Mitigation', icon: ShieldCheck },
  { key: 'funding', title: 'Funding Recommendations', icon: Landmark },
];

export default function RecommendationsPage() {
  const { id } = useParams();
  if (!id) {
    return (
      <div>
        <PageHeader title="AI Recommendations" subtitle="Pick a validation to see personalized recommendations." />
        <ValidationPicker toPath={(vid) => `/validations/${vid}/recommendations`} title="recommendations" />
      </div>
    );
  }
  return <RecommendationsContent id={id} />;
}

function RecommendationsContent({ id }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    validationApi.recommendations(id).then(setData).catch(() => toast.error('Unable to load recommendations.')).finally(() => setLoading(false));
  }, [id]);

  if (loading) return <Loader label="Loading AI recommendations..." />;
  if (!data) return null;

  return (
    <div>
      <PageHeader title="AI Recommendations" subtitle="Personalized guidance based on your actual inputs and results." />
      <div className="card mb-6 border-l-4 border-brand-red">
        <p className="text-sm font-medium text-navy-800">{data.summary}</p>
      </div>
      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
        {SECTIONS.map(({ key, title, icon: Icon }) => (
          <div key={key} className="card">
            <div className="mb-3 flex items-center gap-2">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-navy-50 text-navy-700">
                <Icon size={16} />
              </div>
              <h4 className="text-sm font-bold text-navy-900">{title}</h4>
            </div>
            <ul className="space-y-2">
              {(data[key] || []).map((item, idx) => (
                <li key={idx} className="flex gap-2 text-sm text-slate-600">
                  <span className="mt-1.5 h-1.5 w-1.5 flex-shrink-0 rounded-full bg-brand-red" />
                  {item}
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>
    </div>
  );
}
