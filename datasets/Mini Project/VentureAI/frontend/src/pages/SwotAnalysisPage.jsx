import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { validationApi } from '../api/validation';
import PageHeader from '../components/PageHeader';
import Loader from '../components/Loader';
import SwotCard from '../components/SwotCard';

export default function SwotAnalysisPage() {
  const { id } = useParams();
  const [swot, setSwot] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    validationApi.swot(id).then(setSwot).catch(() => toast.error('Unable to load SWOT analysis.')).finally(() => setLoading(false));
  }, [id]);

  if (loading) return <Loader label="Loading SWOT analysis..." />;
  if (!swot) return null;

  return (
    <div>
      <PageHeader title="SWOT Analysis" subtitle="AI-generated from your actual startup inputs — not a generic template." />
      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
        <SwotCard type="strengths" items={swot.strengths} />
        <SwotCard type="weaknesses" items={swot.weaknesses} />
        <SwotCard type="opportunities" items={swot.opportunities} />
        <SwotCard type="threats" items={swot.threats} />
      </div>
    </div>
  );
}
