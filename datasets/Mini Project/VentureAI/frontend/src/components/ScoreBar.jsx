import React from 'react';

export default function ScoreBar({ label, value = 0 }) {
  const clamped = Math.max(0, Math.min(100, value));
  const color = clamped >= 80 ? 'bg-emerald-500' : clamped >= 60 ? 'bg-amber-500' : clamped >= 40 ? 'bg-orange-500' : 'bg-red-500';
  return (
    <div>
      <div className="mb-1 flex items-center justify-between text-sm">
        <span className="font-medium text-slate-700">{label}</span>
        <span className="font-semibold text-navy-900">{Math.round(clamped)}%</span>
      </div>
      <div className="h-2.5 w-full overflow-hidden rounded-full bg-slate-100">
        <div className={`h-full rounded-full ${color}`} style={{ width: `${clamped}%`, transition: 'width 0.8s ease-out' }} />
      </div>
    </div>
  );
}
