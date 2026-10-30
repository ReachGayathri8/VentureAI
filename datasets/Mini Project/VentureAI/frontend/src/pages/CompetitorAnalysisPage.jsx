import React, { useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { validationApi } from '../api/validation';
import PageHeader from '../components/PageHeader';
import Loader from '../components/Loader';
import ValidationPicker from '../components/ValidationPicker';
import RatingRow from '../components/RatingRow';

export default function CompetitorAnalysisPage() {
  const { id } = useParams();

  if (!id) {
    return (
      <div>
        <PageHeader
          title="Competitor Intelligence"
          subtitle="Pick a validation to compare your startup with relevant competitors."
        />
        <ValidationPicker
          toPath={(vid) => `/validations/${vid}/competitors`}
          title="competitor intelligence"
        />
      </div>
    );
  }

  return <CompetitorAnalysisContent id={id} />;
}

function CompetitorAnalysisContent({ id }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    validationApi
      .competitors(id)
      .then(setData)
      .catch(() => toast.error('Unable to load competitor analysis.'))
      .finally(() => setLoading(false));
  }, [id]);

  const competitors = useMemo(() => {
    if (!data?.competitors) return [];

    const seen = new Set();

    return data.competitors.filter((competitor) => {
      const website = (competitor.website || '')
        .trim()
        .toLowerCase()
        .replace(/^https?:\/\//, '')
        .replace(/^www\./, '')
        .replace(/\/$/, '');

      const name = (competitor.name || '').trim().toLowerCase();
      const key = website || name;

      if (!key || seen.has(key)) return false;

      seen.add(key);
      return true;
    });
  }, [data]);

  if (loading) return <Loader label="Loading competitor intelligence..." />;
  if (!data) return null;

  const columns = ['Your Startup', ...competitors.map((c) => c.name)];
  const yourRatings = [
    data.yourPriceRating,
    data.yourFeatureRating,
    data.yourMarketReachRating,
    data.yourInnovationRating,
  ];

  return (
    <div>
      <PageHeader
        title="Competitor Intelligence"
        subtitle="Compare your startup against relevant known and AI-discovered competitors."
      />

      {competitors.length === 0 ? (
        <div className="card text-sm text-slate-500">
          No relevant competitors were supplied or discovered for this validation.
        </div>
      ) : (
        <div className="card overflow-x-auto">
          <div className="mb-5">
            <h3 className="section-title">Competitor Comparison</h3>
            <p className="mt-1 text-sm text-slate-500">
              Compare price, features, market reach and innovation at a glance.
            </p>
          </div>

          <table className="w-full min-w-[760px] text-left text-sm">
            <thead>
              <tr className="border-b border-slate-100 text-xs uppercase tracking-wide text-slate-400">
                <th className="sticky left-0 z-10 bg-white pb-3 pr-6">Metric</th>
                {columns.map((column, index) => (
                  <th
                    key={`${column}-${index}`}
                    className="min-w-[150px] pb-3 pr-6 align-bottom"
                  >
                    <span className="block leading-5">{column}</span>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              <RatingRow
                label="Price"
                values={[yourRatings[0], ...competitors.map((c) => c.priceRating)]}
              />
              <RatingRow
                label="Features"
                values={[yourRatings[1], ...competitors.map((c) => c.featureRating)]}
              />
              <RatingRow
                label="Market Reach"
                values={[yourRatings[2], ...competitors.map((c) => c.marketReachRating)]}
              />
              <RatingRow
                label="Innovation"
                values={[yourRatings[3], ...competitors.map((c) => c.innovationRating)]}
              />
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}