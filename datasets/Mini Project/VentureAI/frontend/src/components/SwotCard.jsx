import React from 'react';

const THEMES = {
  strengths: { bg: 'bg-emerald-50', border: 'border-emerald-200', text: 'text-emerald-800', title: 'STRENGTHS' },
  weaknesses: { bg: 'bg-red-50', border: 'border-red-200', text: 'text-red-800', title: 'WEAKNESSES' },
  opportunities: { bg: 'bg-sky-50', border: 'border-sky-200', text: 'text-sky-800', title: 'OPPORTUNITIES' },
  threats: { bg: 'bg-amber-50', border: 'border-amber-200', text: 'text-amber-800', title: 'THREATS' },
};

export default function SwotCard({ type, items = [] }) {
  const theme = THEMES[type];
  return (
    <div className={`rounded-2xl border ${theme.border} ${theme.bg} p-5`}>
      <h3 className={`mb-3 text-sm font-extrabold tracking-wide ${theme.text}`}>{theme.title}</h3>
      <ul className="space-y-2">
        {items.length === 0 && <li className="text-sm text-slate-400">No items generated.</li>}
        {items.map((item, idx) => (
          <li key={idx} className={`flex gap-2 text-sm ${theme.text}`}>
            <span className="mt-1 h-1.5 w-1.5 flex-shrink-0 rounded-full bg-current" />
            <span>{item}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
