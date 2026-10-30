import React from 'react';

export default function KpiCard({ label, value, sublabel, icon: Icon, accent = 'navy' }) {
  const accentClasses = {
    navy: 'bg-navy-50 text-navy-700',
    red: 'bg-red-50 text-brand-red',
    emerald: 'bg-emerald-50 text-emerald-700',
    amber: 'bg-amber-50 text-amber-700',
  };
  return (
    <div className="card flex items-center justify-between">
      <div>
        <p className="text-sm font-medium text-slate-500">{label}</p>
        <p className="mt-1 text-2xl font-extrabold text-navy-900">{value}</p>
        {sublabel && <p className="mt-1 text-xs text-slate-400">{sublabel}</p>}
      </div>
      {Icon && (
        <div className={`flex h-12 w-12 items-center justify-center rounded-xl ${accentClasses[accent]}`}>
          <Icon size={22} />
        </div>
      )}
    </div>
  );
}
